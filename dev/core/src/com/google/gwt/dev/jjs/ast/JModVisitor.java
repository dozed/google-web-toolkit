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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.util.collect.Lists;

import java.util.List;

/**
 * A visitor for iterating through and modifying an AST.
 */
public class JModVisitor extends JVisitor {

  /**
   * Context for traversing a mutable list.
   */
  @SuppressWarnings("unchecked")
  private class ListContext<T extends JNode> extends VisitorContext {
    int index;
    final List<T> list;
    boolean removed;
    boolean replaced;

    public ListContext(List<T> list) {
      this.list = list;
    }

    public boolean canInsert() {
      return true;
    }

    public boolean canRemove() {
      return true;
    }

    public void insertAfter(JNode node) {
      checkRemoved();
      list.add(index + 1, (T) node);
      numMods++;
    }

    public void insertBefore(JNode node) {
      checkRemoved();
      list.add(index++, (T) node);
      numMods++;
    }

    public boolean isLvalue() {
      return false;
    }

    public void removeMe() {
      checkState();
      list.remove(index--);
      removed = true;
      numMods++;
    }

    public void replaceMe(JNode node) {
      checkState();
      checkReplacement(list.get(index), node);
      list.set(index, (T) node);
      replaced = true;
      numMods++;
    }

    /**
     * Cause my list to be traversed by this context.
     */
    protected List<T> traverse() {
      try {
        for (index = 0; index < list.size(); ++index) {
          removed = replaced = false;
          list.get(index).traverse(JModVisitor.this, this);
        }
        JModVisitor.this.numVisitorChanges += numMods;
        return list;
      } catch (Throwable e) {
        throw translateException(list.get(index), e);
      }
    }

    private void checkRemoved() {
      if (removed) {
        throw new InternalCompilerException("Node was already removed");
      }
    }

    private void checkState() {
      checkRemoved();
      if (replaced) {
        throw new InternalCompilerException("Node was already replaced");
      }
    }
  }

  /**
   * Context for traversing an immutable list.
   */
  @SuppressWarnings("unchecked")
  private class ListContextImmutable<T extends JNode> extends VisitorContext {
    int index;
    List<T> list;
    boolean removed;
    boolean replaced;

    public ListContextImmutable(List<T> list) {
      this.list = list;
    }

    public boolean canInsert() {
      return true;
    }

    public boolean canRemove() {
      return true;
    }

    public void insertAfter(JNode node) {
      checkRemoved();
      list = Lists.add(list, index + 1, (T) node);
      numMods++;
    }

    public void insertBefore(JNode node) {
      checkRemoved();
      list = Lists.add(list, index++, (T) node);
      numMods++;
    }

    public boolean isLvalue() {
      return false;
    }

    public void removeMe() {
      checkState();
      list = Lists.remove(list, index--);
      removed = true;
      numMods++;
    }

    public void replaceMe(JNode node) {
      checkState();
      checkReplacement(list.get(index), node);
      list = Lists.set(list, index, (T) node);
      replaced = true;
      numMods++;
    }

    /**
     * Cause my list to be traversed by this context.
     */
    protected List<T> traverse() {
      try {
        for (index = 0; index < list.size(); ++index) {
          removed = replaced = false;
          list.get(index).traverse(JModVisitor.this, this);
        }
        numVisitorChanges += numMods;
        return list;
      } catch (Throwable e) {
        throw translateException(list.get(index), e);
      }
    }

    private void checkRemoved() {
      if (removed) {
        throw new InternalCompilerException("Node was already removed");
      }
    }

    private void checkState() {
      checkRemoved();
      if (replaced) {
        throw new InternalCompilerException("Node was already replaced");
      }
    }
  }

  private class LvalueContext extends NodeContext {
    public LvalueContext() {
      super(false);
    }

    @Override
    public boolean isLvalue() {
      return true;
    }
  }

  private static class NodeContext extends VisitorContext {
    boolean canRemove;
    JNode node;
    boolean replaced;

    public NodeContext(boolean canRemove) {
      this.canRemove = canRemove;
    }

    public boolean canInsert() {
      return false;
    }

    public boolean canRemove() {
      return this.canRemove;
    }

    public void insertAfter(JNode node) {
      throw new UnsupportedOperationException("Can't insert after " + node);
    }

    public void insertBefore(JNode node) {
      throw new UnsupportedOperationException("Can't insert before " + node);
    }

    public boolean isLvalue() {
      return false;
    }

    public void removeMe() {
      if (!canRemove) {
        throw new UnsupportedOperationException("Can't remove " + node);
      }

      this.node = null;
      numMods++;
    }

    public void replaceMe(JNode node) {
      if (replaced) {
        throw new InternalCompilerException("Node was already replaced");
      }
      checkReplacement(this.node, node);
      this.node = node;
      replaced = true;
      numMods++;
    }
  }

  private abstract static class VisitorContext implements Context {
    int numMods = 0;

    public int getNumMods() {
      return numMods;
    }
  }

  protected static void checkReplacement(JNode origNode, JNode newNode) {
    if (newNode == null) {
      throw new InternalCompilerException("Cannot replace with null");
    }
    if (newNode == origNode) {
      throw new InternalCompilerException("The replacement is the same as the original");
    }
  }

  private int numVisitorChanges = 0;
  
  @Override
  public JNode accept(JNode node) {
    return accept(node, false);
  }

  @Override
  public JNode accept(JNode node, boolean allowRemove) {
    NodeContext ctx = new NodeContext(allowRemove);
    try {
      ctx.node = node;
      traverse(node, ctx);
      numVisitorChanges += ctx.getNumMods();
      return ctx.node;
    } catch (Throwable e) {
      throw translateException(node, e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends JNode> void accept(List<T> list) {
    NodeContext ctx = new NodeContext(false);
    try {
      for (int i = 0, c = list.size(); i < c; ++i) {
        ctx.node = list.get(i);
        traverse(ctx.node, ctx);
        if (ctx.replaced) {
          list.set(i, (T) ctx.node);
          ctx.replaced = false;
        }
      }
      numVisitorChanges += ctx.getNumMods();
    } catch (Throwable e) {
      throw translateException(ctx.node, e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends JNode> List<T> acceptImmutable(List<T> list) {
    NodeContext ctx = new NodeContext(false);
    try {
      for (int i = 0, c = list.size(); i < c; ++i) {
        ctx.node = list.get(i);
        traverse(ctx.node, ctx);
        if (ctx.replaced) {
          list = Lists.set(list, i, (T) ctx.node);
          ctx.replaced = false;
        }
      }
      numVisitorChanges += ctx.getNumMods();
      return list;
    } catch (Throwable e) {
      throw translateException(ctx.node, e);
    }
  }

  @Override
  public JExpression acceptLvalue(JExpression expr) {
    LvalueContext ctx = new LvalueContext();
    try {
      ctx.node = expr;
      traverse(expr, ctx);
      numVisitorChanges += ctx.getNumMods();
      return (JExpression) ctx.node;
    } catch (Throwable e) {
      throw translateException(expr, e);
    }
  }

  @Override
  public <T extends JNode> void acceptWithInsertRemove(List<T> list) {
    new ListContext<T>(list).traverse();
  }

  @Override
  public <T extends JNode> List<T> acceptWithInsertRemoveImmutable(List<T> list) {
    return new ListContextImmutable<T>(list).traverse();
  }

  @Override
  public final boolean didChange() {
    return numVisitorChanges > 0;
  }

  /**
   * Returns the number of times the tree was changed since this visitor was
   * instantiated.
   */
  public int getNumMods() {
    return numVisitorChanges;
  }

  /**
   * Call this method to indicate that a visitor has made a change to the tree.
   * Used to determine when the optimization pass can exit.
   */
  protected void madeChanges() {
    numVisitorChanges++;
  }
  
  /**
   * Call this method to indicate that a visitor has made multiple changes
   * to the tree.  Used to determine when the optimization pass can exit.
   */
  protected void madeChanges(int numMods) {
    numVisitorChanges += numMods;
  }

  protected void traverse(JNode node, Context context) {
    node.traverse(this, context);
  }
}
