/*
 * Copyright 2010 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package com.google.java.contract.core.util;

import com.google.java.contract.ContractImport;
import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ClassName;

import java.util.TreeMap;

/**
 * A map that stores associations between patterns and rules. Patterns
 * can overlap: the last registered pattern overrides previous ones.
 *
 * <p>Patterns are semi-qualified names (nested classes have their
 * names flattened), optionally followed by {@code .*}. A normal
 * pattern matches itself exactly. A star pattern matches any class
 * whose name begins with the pattern minus the terminating
 * {@code .*}.
 *
 * <p>The characters {@code .} and {@code /} can be used
 * interchangeably in patterns and names.
 *
 * <p><i>Implementation note:</i> This implementation uses a ternary
 * search tree based on {@link TreeMap}.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @param <R> the type of a rule
 */
@ContractImport({
  "com.google.java.contract.core.model.ClassName",
  "com.google.java.contract.util.Iterables",
  "com.google.java.contract.util.Predicates"
})
@Invariant("root != null")
public class PatternMap<R> {
  /**
   * A node in the internal ternary search tree.
   */
  @Invariant({
    "children != null",
    "Iterables.all(children.keySet(), ClassName.isSimpleName())",
    "!children.values().contains(null)"
  })
  protected class TernaryNode {
    protected TreeMap<String, TernaryNode> children;
    protected R rule;
    protected boolean exact;

    protected TernaryNode(R rule, boolean exact) {
      children = new TreeMap<String, TernaryNode>();
      this.rule = rule;
      this.exact = exact;
    }
  }

  protected TernaryNode root;

  public PatternMap() {
    root = new TernaryNode(null, false);
  }

  /**
   * Returns the rule associated with {@code pattern}.
   */
  @Requires("isValidPattern(pattern)")
  public R get(String pattern) {
    String canon = pattern.replace('/', '.');
    boolean exact = !canon.endsWith(".*");
    if (!exact) {
      canon = canon.substring(0, canon.length() - 2);
    }

    String[] parts = canon.split("\\.");
    TernaryNode current = root;
    R best = null;
    for (int i = 0; i < parts.length; ++i) {
      TernaryNode next = current.children.get(parts[i]);
      if (next == null) {
        break;
      }
      if (next.rule != null) {
        if (next.exact) {
          if (exact && i == parts.length - 1) {
            best = next.rule;
          }
        } else {
          best = next.rule;
        }
      }
      current = next;
    }
    return best;
  }

  /**
   * Returns {@code true} if {@code pattern} has another pattern
   * overriding it, that is if there exists some name that is matched
   * by {@code pattern} and is associated with a different rule than
   * the one associated with {@code pattern}.
   *
   * <p>For example, following the calls {@code put("a.*", r1)} and
   * {@code put("a.b.*", r2)}, {@code isOverriden("a.*")} will return
   * {@code true} if {@code !r1.equals(r2)}.
   *
   * <p>This method always returns {@code false} if {@code pattern} is
   * exact.
   */
  @Requires("isValidPattern(pattern)")
  public boolean isOverriden(String pattern) {
    String canon = pattern.replace('/', '.');
    boolean exact = !canon.endsWith(".*");
    if (exact) {
      return false;
    }
    canon = canon.substring(0, canon.length() - 2);

    String[] parts = canon.split("\\.");
    TernaryNode current = root;
    for (int i = 0; i < parts.length; ++i) {
      TernaryNode next = current.children.get(parts[i]);
      if (next == null) {
        return false;
      }
      current = next;
    }
    return !current.children.isEmpty();
  }

  /**
   * Associates {@code rule} with {@code pattern}.
   */
  @Requires({
    "isValidPattern(pattern)",
    "rule != null"
  })
  @Ensures({
    "rule.equals(get(pattern))",
    "!isOverriden(pattern)"
  })
  public void put(String pattern, R rule) {
    String canon = pattern.replace('/', '.');
    boolean exact = !canon.endsWith(".*");
    if (!exact) {
      canon = canon.substring(0, canon.length() - 2);
    }

    String[] parts = canon.split("\\.");

    TernaryNode current = root;
    TernaryNode parent = root;
    int parentIndex = -1;
    for (int i = 0; i < parts.length; ++i) {
      TernaryNode next = current.children.get(parts[i]);
      if (next == null) {
        next = new TernaryNode(null, false);
        current.children.put(parts[i], next);
      }
      if (next.rule != null && !next.exact) {
        parent = next;
        parentIndex = i;
      }
      current = next;
    }

    if (!exact) {
      /* Override patterns beginning with this one. */
      current.children.clear();
    }

    if (!rule.equals(parent.rule)) {
      current.rule = rule;
      current.exact = exact;
    } else {
      /*
       * The new rule we are inserting is already specified through
       * inheritance. Remove spurious nodes. This ensures that any
       * overriding branch is meaningful; that is, it specifies a rule
       * value different from the inherited one.
       */
      current = parent;
      for (int i = parentIndex + 1; i < parts.length; ++i) {
        TernaryNode next = current.children.get(parts[i]);
        if (next.rule == null) {
          current.children.remove(parts[i]);
          break;
        }
      }
    }
  }

  /**
   * Returns {@code true} if {@code pattern} is a valid pattern.
   */
  public static boolean isValidPattern(String pattern) {
    if (pattern == null) {
      return false;
    }
    String canon = pattern.replace('/', '.');
    return ClassName.isQualifiedName(canon)
        || ClassName.isStarQualifiedName(canon);
  }
}
