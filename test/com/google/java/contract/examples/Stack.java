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
package com.google.java.contract.examples;

import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;

/**
 * A simple stack interface demonstrating the use of contracts. This
 * interface does not extend java.lang.Collection as we are lazy and
 * want a small example. Besides, Collection itself is not contracted,
 * and some of the contracts would more logically fit there than in
 * this interface.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @param <T> the type of an element
 */
@Invariant("size() >= 0")
public interface Stack<T> {
  /**
   * Returns the number of elements in this stack.
   */
  public int size();

  /**
   * Returns the topmost element of this stack without removing it.
   */
  @Requires("size() >= 1")
  public T peek();

  /**
   * Pops the topmost element off this stack.
   */
  @Requires("size() >= 1")
  @Ensures({
    "size() == old (size()) - 1",
    "result == old (peek())"
  })
  public T pop();

  /**
   * Pushes an element onto the stack.
   */
  @Ensures({
    "size() == old (size()) + 1",
    "peek() == old (obj)"
  })
  public void push(T obj);
}
