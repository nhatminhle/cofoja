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
package com.google.java.contract;

/**
 * An object that exposes methods to alter the contracting
 * environment. Any changes made to the environment are only
 * guaranteed to take effect on future actions; for example, disabling
 * contracts on an already loaded class has no effect.
 *
 * <p>Methods that match multiple classes accept patterns. Patterns
 * are semi-qualified names (nested classes have their names
 * flattened), optionally followed by {@code .*}. A normal pattern
 * matches itself exactly. A star pattern matches any class whose name
 * begins with the pattern minus the terminating {@code .*}. In case
 * of pattern overlap, subsequent rules override previous ones.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public interface ContractEnvironment {
  /**
   * Enables precondition checking for classes matched by pattern
   * {@code pattern}.
   *
   * @throws UnsupportedOperationException if this environment does
   * not support selective contract activation
   */
  @Requires("pattern != null")
  @Ensures("hasPreconditionsEnabled(pattern)")
  public void enablePreconditions(String pattern);

  /**
   * Disables precondition checking for classes matched by pattern
   * {@code pattern}.
   *
   * @throws UnsupportedOperationException if this environment does
   * not support selective contract activation
   */
  @Requires("pattern != null")
  @Ensures("!hasPreconditionsEnabled(pattern)")
  public void disablePreconditions(String pattern);

  /**
   * Enables postcondition checking for classes matched by pattern
   * {@code pattern}.
   *
   * @throws UnsupportedOperationException if this environment does
   * not support selective contract activation
   */
  @Requires("pattern != null")
  @Ensures("hasPostconditionsEnabled(pattern)")
  public void enablePostconditions(String pattern);

  /**
   * Disables postcondition checking for classes matched by pattern
   * {@code pattern}.
   *
   * @throws UnsupportedOperationException if this environment does
   * not support selective contract activation
   */
  @Requires("pattern != null")
  @Ensures("!hasPostconditionsEnabled(pattern)")
  public void disablePostconditions(String pattern);

  /**
   * Enables invariant checking for classes matched by pattern
   * {@code pattern}.
   *
   * @throws UnsupportedOperationException if this environment does
   * not support selective contract activation
   */
  @Requires("pattern != null")
  @Ensures("hasInvariantsEnabled(pattern)")
  public void enableInvariants(String pattern);

  /**
   * Disables invariant checking for classes matched by pattern
   * {@code pattern}.
   *
   * @throws UnsupportedOperationException if this environment does
   * not support selective contract activation
   */
  @Requires("pattern != null")
  @Ensures("!hasInvariantsEnabled(pattern)")
  public void disableInvariants(String pattern);

  /**
   * Returns {@code true} if {@code clazz} has preconditions
   * enabled. This does <em>not</em> imply that such a class has any
   * preconditions at all.
   */
  @Requires("clazz != null")
  public boolean hasPreconditionsEnabled(Class<?> clazz);

  /**
   * Returns {@code true} if all classes matched by {@code pattern}
   * have preconditions enabled. This does <em>not</em> imply that
   * such a class has any preconditions at all.
   */
  @Requires("pattern != null")
  public boolean hasPreconditionsEnabled(String pattern);

  /**
   * Returns {@code true} if {@code clazz} has postconditions
   * enabled. This does <em>not</em> imply that such a class has any
   * postconditions at all.
   */
  @Requires("clazz != null")
  public boolean hasPostconditionsEnabled(Class<?> clazz);

  /**
   * Returns {@code true} if all classes matched by {@code pattern}
   * have postconditions enabled. This does <em>not</em> imply that
   * such a class has any postconditions at all.
   */
  @Requires("pattern != null")
  public boolean hasPostconditionsEnabled(String pattern);

  /**
   * Returns {@code true} if {@code clazz} has invariants
   * enabled. This does <em>not</em> imply that such a class has any
   * invariants at all.
   */
  @Requires("clazz != null")
  public boolean hasInvariantsEnabled(Class<?> clazz);

  /**
   * Returns {@code true} if all classes matched by {@code pattern}
   * have invariants enabled. This does <em>not</em> imply that such a
   * class has any invariants at all.
   */
  @Requires("pattern != null")
  public boolean hasInvariantsEnabled(String pattern);

  /**
   * Ignores classes matched by {@code pattern}. Ignored classes are
   * not touched by Contracts for Java in any way: they are neither
   * loaded nor examined for contracts.
   *
   * <p>If you are looking for a method to disable contracts, this is
   * <em>not</em> the right method; use methods such as
   * {@link #disableInvariants(String)} instead.
   *
   * <p>By default, the following classes are ignored:
   *
   * <ul>
   * <li>{@code java.*}
   * <li>{@code javax.*}
   * <li>{@code com.sun.*}
   * <li>{@code sun.*}
   * </ul>
   */
  @Requires("pattern != null")
  @Ensures("isIgnored(pattern)")
  public void ignore(String pattern);

  /**
   * Unignore classes matched by {@code pattern}. This method can be
   * used to unignore some classes that have previously been ignored,
   * or that are ignored by default.
   *
   * @see #ignore(String)
   */
  @Requires("pattern != null")
  @Ensures("!isIgnored(pattern)")
  public void unignore(String pattern);

  /**
   * Returns {@code true} if all classes matched by {@code pattern}
   * are ignored.
   *
   * @see #ignore(String)
   */
  @Requires("pattern != null")
  public boolean isIgnored(String pattern);
}
