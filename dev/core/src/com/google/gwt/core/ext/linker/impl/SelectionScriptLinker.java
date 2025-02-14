/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.SoftPermutation;
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.dev.util.Util;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * A base class for Linkers that use an external script to boostrap the GWT
 * module. This implementation injects JavaScript Snippets into a JS program
 * defined in an external file.
 */
public abstract class SelectionScriptLinker extends AbstractLinker {
  /**
   * TODO(bobv): Move this class into c.g.g.core.linker when HostedModeLinker
   * goes away?
   */

  /**
   * A configuration property indicating how large each script tag should be.
   */
  private static final String CHUNK_SIZE_PROPERTY = "iframe.linker.script.chunk.size";

  /**
   * File name for computeScriptBase.js.
   */
  protected static final String COMPUTE_SCRIPT_BASE_JS = "com/google/gwt/core/ext/linker/impl/computeScriptBaseOld.js";

  /**
   * The extension added to demand-loaded fragment files.
   */
  protected static final String FRAGMENT_EXTENSION = ".cache.js";

  /**
   * A subdirectory to hold all the generated fragments.
   */
  protected static final String FRAGMENT_SUBDIR = "deferredjs";

  /**
   * Utility class to handle insertion of permutations code.
   */
  protected static PermutationsUtil permutationsUtil = new PermutationsUtil();

  /**
   * File name for processMetas.js.
   */
  protected static final String PROCESS_METAS_JS = "com/google/gwt/core/ext/linker/impl/processMetasOld.js";

  /**
   * A configuration property that can be used to have the linker load from
   * somewhere other than {@link #FRAGMENT_SUBDIR}.
   */
  private static final String PROP_FRAGMENT_SUBDIR_OVERRIDE = "iframe.linker.deferredjs.subdir";

  protected static void replaceAll(StringBuffer buf, String search,
      String replace) {
    int len = search.length();
    for (int pos = buf.indexOf(search); pos >= 0; pos = buf.indexOf(search,
        pos + 1)) {
      buf.replace(pos, pos + len, replace);
    }
  }

  /**
   * Split a JavaScript string into multiple chunks, at statement boundaries.
   * This method is made default access for testing.
   * 
   * @param ranges Describes where the statements are located within the
   *          JavaScript code. If <code>null</code>, then return <code>js</code>
   *          unchanged.
   * @param js The JavaScript code to be split up.
   * @param charsPerChunk The number of characters to be put in each script tag.
   * @param scriptChunkSeparator The string to insert between chunks.
   */
  public static String splitPrimaryJavaScript(StatementRanges ranges, String js,
      int charsPerChunk, String scriptChunkSeparator) {
    if (charsPerChunk < 0 || ranges == null) {
      return js;
    }

    StringBuilder sb = new StringBuilder();
    int bytesInCurrentChunk = 0;

    for (int i = 0; i < ranges.numStatements(); i++) {
      int start = ranges.start(i);
      int end = ranges.end(i);
      int length = end - start;
      if (bytesInCurrentChunk > 0 && bytesInCurrentChunk + length > charsPerChunk) {
        if (lastChar(sb) != '\n') {
          sb.append('\n');
        }
        sb.append(scriptChunkSeparator);
        bytesInCurrentChunk = 0;
      }
      if (bytesInCurrentChunk > 0) {
        char lastChar = lastChar(sb);
        if (lastChar != '\n' && lastChar != ';' && lastChar != '}') {
          /*
           * Make sure this statement has a separator from the last one.
           */
          sb.append(";");
        }
      }
      sb.append(js, start, end);
      bytesInCurrentChunk += length;
    }
    return sb.toString();
  }

  private static char lastChar(StringBuilder sb) {
    return sb.charAt(sb.length() - 1);
  }

  /**
   * This method is left in place for existing subclasses of
   * SelectionScriptLinker that have not been upgraded for the sharding API.
   */
  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    ArtifactSet toReturn = link(logger, context, artifacts, true);
    toReturn = link(logger, context, toReturn, false);
    return toReturn;
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts, boolean onePermutation)
      throws UnableToCompleteException {
    if (onePermutation) {
      ArtifactSet toReturn = new ArtifactSet(artifacts);

      /*
       * Support having multiple compilation results because this method is also
       * called from the legacy link method.
       */
      for (CompilationResult compilation : toReturn.find(CompilationResult.class)) {
        toReturn.addAll(doEmitCompilation(logger, context, compilation, artifacts));
        maybeAddHostedModeFile(logger, context, toReturn, compilation);
      }
      return toReturn;
    } else {
      permutationsUtil.setupPermutationsMap(artifacts);
      ArtifactSet toReturn = new ArtifactSet(artifacts);
      EmittedArtifact art = emitSelectionScript(logger, context, artifacts);
      if (art != null) {
        toReturn.add(art);
      }
      maybeOutputPropertyMap(logger, context, toReturn);
      maybeAddHostedModeFile(logger, context, toReturn, null);
      return toReturn;
    }
  }

  @Override
  public boolean supportsDevModeInJunit(LinkerContext context) {
    return (getHostedFilename() != "");
  }

  /**
   * Extract via {@link #CHUNK_SIZE_PROPERTY} the number of characters to be
   * included in each chunk.
   */
  protected int charsPerChunk(LinkerContext context, TreeLogger logger) {
    SortedSet<ConfigurationProperty> configProps = context.getConfigurationProperties();
    for (ConfigurationProperty prop : configProps) {
      if (prop.getName().equals(CHUNK_SIZE_PROPERTY)) {
        return Integer.parseInt(prop.getValues().get(0));
      }
    }
    // CompilerParameters.gwt.xml indicates that if this property is -1, then
    // no chunking is performed, so we return that as the default.  Since
    // Core.gwt.xml contains a definition for this property, this should never
    // happen in production, but some tests mock out the ConfigurationProperties
    // so we want to have a reasonable default rather than making them all add
    // a value for this property.
    return -1;
  }

  protected Collection<Artifact<?>> doEmitCompilation(TreeLogger logger,
      LinkerContext context, CompilationResult result, ArtifactSet artifacts)
      throws UnableToCompleteException {
    String[] js = result.getJavaScript();
    byte[][] bytes = new byte[js.length][];
    bytes[0] = generatePrimaryFragment(logger, context, result, js, artifacts);
    for (int i = 1; i < js.length; i++) {
      bytes[i] = Util.getBytes(generateDeferredFragment(logger, context, i,
          js[i]));
    }

    Collection<Artifact<?>> toReturn = new ArrayList<Artifact<?>>();
    toReturn.add(emitBytes(logger, bytes[0], result.getStrongName()
        + getCompilationExtension(logger, context)));
    for (int i = 1; i < js.length; i++) {
      toReturn.add(emitBytes(logger, bytes[i], FRAGMENT_SUBDIR + File.separator
          + result.getStrongName() + File.separator + i + FRAGMENT_EXTENSION));
    }

    toReturn.addAll(emitSelectionInformation(result.getStrongName(), result));
 
    return toReturn;
  }

  protected EmittedArtifact emitSelectionScript(TreeLogger logger,
      LinkerContext context, ArtifactSet artifacts)
      throws UnableToCompleteException {
    /*
     * Last modified is important to keep Development Mode refreses from
     * clobbering Production Mode compiles. We set the timestamp on the
     * Development Mode selection script to the same mod time as the module (to
     * allow updates). For Production Mode, we just set it to now.
     */
    long lastModified;
    if (permutationsUtil.getPermutationsMap().isEmpty()) {
      lastModified = context.getModuleLastModified();
    } else {
      lastModified = System.currentTimeMillis();
    }
    String ss = generateSelectionScript(logger, context, artifacts);
    return emitString(logger, ss, context.getModuleName()
        + ".nocache.js", lastModified);
  }
  
  /**
   * Generate a selection script. The selection information should previously
   * have been scanned using {@link PermutationsUtil#setupPermutationsMap(ArtifactSet)}.
   */
  protected String fillSelectionScriptTemplate(StringBuffer selectionScript,
      TreeLogger logger, LinkerContext context, ArtifactSet artifacts,
      CompilationResult result) throws
      UnableToCompleteException {
    String computeScriptBase;
    String processMetas;
    try {
      computeScriptBase = Utility.getFileFromClassPath(COMPUTE_SCRIPT_BASE_JS);
      processMetas = Utility.getFileFromClassPath(PROCESS_METAS_JS);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to read selection script template",
          e);
      throw new UnableToCompleteException();
    }
    replaceAll(selectionScript, "__COMPUTE_SCRIPT_BASE__", computeScriptBase);
    replaceAll(selectionScript, "__PROCESS_METAS__", processMetas);
    
    selectionScript = ResourceInjectionUtil.injectResources(selectionScript, artifacts);
    permutationsUtil.addPermutationsJs(selectionScript, logger, context);
    
    replaceAll(selectionScript, "__MODULE_FUNC__",
        context.getModuleFunctionName());
    replaceAll(selectionScript, "__MODULE_NAME__", context.getModuleName());
    replaceAll(selectionScript, "__HOSTED_FILENAME__", getHostedFilename());

    return selectionScript.toString();
  }
  
  /**
   * @param logger a TreeLogger
   * @param context a LinkerContext
   * @param fragment the fragment number
   */
  protected String generateDeferredFragment(TreeLogger logger,
      LinkerContext context, int fragment, String js) {
    return js;
  }

  /**
   * Generate the primary fragment. The default implementation is based on
   * {@link #getModulePrefix(TreeLogger, LinkerContext, String, int)} and
   * {@link #getModuleSuffix(TreeLogger, LinkerContext)}.
   */
  protected byte[] generatePrimaryFragment(TreeLogger logger,
      LinkerContext context, CompilationResult result, String[] js,
      ArtifactSet artifacts) throws UnableToCompleteException {
    TextOutput to = new DefaultTextOutput(context.isOutputCompact());
    String temp = splitPrimaryJavaScript(result.getStatementRanges()[0], js[0],
        charsPerChunk(context, logger), getScriptChunkSeparator(logger, context));
    to.print(generatePrimaryFragmentString(
        logger, context, result, temp, js.length, artifacts));
    return Util.getBytes(to.toString());
  }
  
  protected String generatePrimaryFragmentString(TreeLogger logger,
      LinkerContext context, CompilationResult result, String js, int length,
      ArtifactSet artifacts) 
  throws UnableToCompleteException {
    StringBuffer b = new StringBuffer();
    String strongName = result == null ? "" : result.getStrongName();
    b.append(getModulePrefix(logger, context, strongName, length));
    b.append(js);
    b.append(getModuleSuffix(logger, context));
    return wrapPrimaryFragment(logger, context, b.toString(), artifacts, result);
  }
  
  protected String generateSelectionScript(TreeLogger logger,
      LinkerContext context, ArtifactSet artifacts) throws UnableToCompleteException {
    return generateSelectionScript(logger, context, artifacts, null);
  }
  
  protected String generateSelectionScript(TreeLogger logger,
        LinkerContext context, ArtifactSet artifacts, CompilationResult result)
        throws UnableToCompleteException {
    String selectionScriptText;
    StringBuffer buffer = readFileToStringBuffer(
        getSelectionScriptTemplate(logger,context), logger);
    selectionScriptText = fillSelectionScriptTemplate(
        buffer, logger, context, artifacts, result);
    selectionScriptText =
      context.optimizeJavaScript(logger, selectionScriptText);
    return selectionScriptText;
  }
  
  protected abstract String getCompilationExtension(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException;

  /**
   * Returns the subdirectory name to be used by getModulPrefix when requesting
   * a runAsync module. It is specified by
   * {@link #PROP_FRAGMENT_SUBDIR_OVERRIDE} and, aside from test cases, is
   * always {@link #FRAGMENT_SUBDIR}.
   */
  protected final String getFragmentSubdir(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException {
    String subdir = null;
    for (ConfigurationProperty prop : context.getConfigurationProperties()) {
      if (prop.getName().equals(PROP_FRAGMENT_SUBDIR_OVERRIDE)) {
        subdir = prop.getValues().get(0);
      }
    }

    if (subdir == null) {
      logger.log(TreeLogger.ERROR, "Could not find property "
          + PROP_FRAGMENT_SUBDIR_OVERRIDE);
      throw new UnableToCompleteException();
    }

    return subdir;
  }
  
  protected String getHostedFilename() {
    return "";
  }
  
  /**
   * Compute the beginning of a JavaScript file that will hold the main module
   * implementation.
   */
  protected abstract String getModulePrefix(TreeLogger logger,
      LinkerContext context, String strongName)
      throws UnableToCompleteException;
  
  /**
   * Compute the beginning of a JavaScript file that will hold the main module
   * implementation. By default, calls
   * {@link #getModulePrefix(TreeLogger, LinkerContext, String)}.
   * 
   * @param strongName strong name of the module being emitted
   * @param numFragments the number of fragments for this module, including the
   *          main fragment (fragment 0)
   */
  protected String getModulePrefix(TreeLogger logger, LinkerContext context,
      String strongName, int numFragments) throws UnableToCompleteException {
    return getModulePrefix(logger, context, strongName);
  }

  protected abstract String getModuleSuffix(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException;

  /**
   * Some subclasses support "chunking" of the primary fragment. If chunking will
   * be supported, this function should be overridden to return the string which
   * should be inserted between each chunk.
   */
  protected String getScriptChunkSeparator(TreeLogger logger, LinkerContext context) {
    return "";
  }

  protected abstract String getSelectionScriptTemplate(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException;
  
  /**
   * Add the Development Mode file to the artifact set.
   */
  protected void maybeAddHostedModeFile(TreeLogger logger, 
      LinkerContext context, ArtifactSet artifacts, CompilationResult result)
      throws UnableToCompleteException {
    String hostedFilename = getHostedFilename();
    if ("".equals(hostedFilename) || result != null) {
      return;
    }
    try {
      URL resource = SelectionScriptLinker.class.getResource(hostedFilename);
      if (resource == null) {
        logger.log(TreeLogger.ERROR,
            "Unable to find support resource: " + hostedFilename);
        throw new UnableToCompleteException();
      }

      final URLConnection connection = resource.openConnection();
      // TODO: extract URLArtifact class?
      EmittedArtifact hostedFile = new EmittedArtifact(
          SelectionScriptLinker.class, hostedFilename) {
        @Override
        public InputStream getContents(TreeLogger logger)
            throws UnableToCompleteException {
          try {
            return connection.getInputStream();
          } catch (IOException e) {
            logger.log(TreeLogger.ERROR, "Unable to copy support resource", e);
            throw new UnableToCompleteException();
          }
        }

        @Override
        public long getLastModified() {
          return connection.getLastModified();
        }
      };
      artifacts.add(hostedFile);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to copy support resource", e);
      throw new UnableToCompleteException();
    }
  }

  protected void maybeOutputPropertyMap(TreeLogger logger,
      LinkerContext context, ArtifactSet toReturn) {
    return;
  }

  protected StringBuffer readFileToStringBuffer(String filename,
      TreeLogger logger) throws UnableToCompleteException {
    StringBuffer buffer;
    try {
      buffer = new StringBuffer(Utility.getFileFromClassPath(filename));
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to read file: " + filename, e);
      throw new UnableToCompleteException();
    }
    return buffer;
  }

  protected String wrapPrimaryFragment(TreeLogger logger,
      LinkerContext context, String script, ArtifactSet artifacts,
      CompilationResult result) throws UnableToCompleteException {
    return script;
  }

  private List<Artifact<?>> emitSelectionInformation(String strongName,
      CompilationResult result) {
    List<Artifact<?>> emitted = new ArrayList<Artifact<?>>();

    for (SortedMap<SelectionProperty, String> propertyMap : result.getPropertyMap()) {
      TreeMap<String, String> propMap = new TreeMap<String, String>();
      for (Map.Entry<SelectionProperty, String> entry : propertyMap.entrySet()) {
        propMap.put(entry.getKey().getName(), entry.getValue());
      }

      // The soft properties may not be a subset of the existing set
      for (SoftPermutation soft : result.getSoftPermutations()) {
        // Make a copy we can add add more properties to
        TreeMap<String, String> softMap = new TreeMap<String, String>(propMap);
        // Make sure this SelectionInformation contains the soft properties
        for (Map.Entry<SelectionProperty, String> entry : soft.getPropertyMap().entrySet()) {
          softMap.put(entry.getKey().getName(), entry.getValue());
        }
        emitted.add(new SelectionInformation(strongName, soft.getId(), softMap));
      }
    }

    return emitted;
  }

}
