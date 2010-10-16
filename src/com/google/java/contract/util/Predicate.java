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
 * A function-object that returns a boolean.
 *
 * <p>This interface is intentionally name-compatible with
 * {@link com.google.common.base.Predicate}, so as to make it easy to
 * switch between them through static imports.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @param <T> the type of input to this predicate
 */
public interface Predicate<T> {
  /**
   * Applies this predicate to {@code obj}.
   */
  public boolean apply(T obj);
}
