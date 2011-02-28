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

import java.util.ArrayList;

/**
 * An implementation of the {@link Stack} example interface using an
 * {@link ArrayList}, demonstrating the use of contracts.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @param <T> the type of an element
 */
@Invariant("elements != null")
public class ArrayListStack<T> implements Stack<T> {
  protected ArrayList<T> elements;

  public ArrayListStack() {
    elements = new ArrayList<T>();
  }

  @Override
  public int size() {
    return elements.size();
  }

  @Override
  public T peek() {
    return elements.get(elements.size() - 1);
  }

  @Override
  public T pop() {
    return elements.remove(elements.size() - 1);
  }

  @Override
  @Ensures("elements.contains(old (obj))")
  public void push(T obj) {
    elements.add(obj);
  }
}
