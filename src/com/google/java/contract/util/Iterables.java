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
package com.google.java.contract.util;

/**
 * Utilities for iterables.
 *
 * <p>These methods are intentionally name-compatible with
 * {@link com.google.common.collect.Iterables}, so as to make it easy
 * to switch between them.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public final class Iterables {
  private Iterables() {
  }

  /**
   * Applies {@code all(p)} to {@code it}.
   *
   * <p><b>Warning:</b> this method accepts a {@code null} iterable
   * object and returns {@code false}. The similarly-named method in
   * {@link com.google.common.collect.Iterables} does <em>not</em> take
   * {@code null} as input.
   */
  public static <T> boolean all(Iterable<T> it, Predicate<? super T> p) {
    if (it == null) {
      return false;
    }
    for (T elem : it) {
      if (!p.apply(elem)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Applies {@code any(p)} to {@code it}.
   *
   * <p><b>Warning:</b> this method accepts a {@code null} iterable
   * object and returns {@code false}. The similarly-named method in
   * {@link com.google.common.collect.Iterables} does <em>not</em> take
   * {@code null} as input.
   */
  public static <T> boolean any(Iterable<T> it, Predicate<? super T> p) {
    if (it == null) {
      return false;
    }
    for (T elem : it) {
      if (p.apply(elem)) {
        return true;
      }
    }
    return false;
  }
}
