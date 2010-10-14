/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.safehtml.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A tag interface that facilitates compile-time binding of HTML templates to
 * generate SafeHtml strings.
 *
 * <p>Example usage:
 * <pre>
 *   public interface MyTemplate extends SafeHtmlTemplates {
 *     &#064;Template("&lt;span class=\"{3}\"&gt;{0}: &lt;a href=\"{1}\"&gt;{2}&lt;/a&gt;&lt;/span&gt;")
 *     SafeHtml messageWithLink(SafeHtml message, String url, String linkText,
 *       String style);
 *   }
 *
 *   private static final MyTemplate TEMPLATE = GWT.create(MyTemplate.class);
 *
 *   public void useTemplate(...) {
 *     SafeHtml message;
 *     String url;
 *     String linkText;
 *     String style;
 *     // ...
 *     SafeHtml messageWithLink =
 *       TEMPLATE.messageWithLink(message, url, linkText, style);
 *   }
 * </pre>
 *
 * <p>
 * Instantiating a {@code SafeHtmlTemplates} interface with {@code GWT.create()}
 * returns an instance of an implementation that is generated at compile time.
 * The code generator parses the value of each template method's
 * {@code &#064;Template} annotation as a (X)HTML template, with template
 * variables denoted by curly-brace placeholders that refer by index to the
 * corresponding template method parameter.
 *
 * <p>
 * <b>Note:</b> The current implementation of the code generator cannot
 * guarantee the {@code SafeHtml} contract for templates with template variables
 * in a CSS or JavaScript context (that is, within a {@code style} attribute or
 * tag; or within {@code &lt;script&gt;} tags or {@code onClick}, {@code
 * onError}, etc. attributes). Developers are advised to avoid such templates,
 * or to review the uses of corresponding template methods very carefully to
 * ensure that values passed into the CSS or JavaScript context cannot result in
 * unintended script execution.
 */
public interface SafeHtmlTemplates {

  /**
   * The HTML template.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface Template {
    String value();
  }
}
