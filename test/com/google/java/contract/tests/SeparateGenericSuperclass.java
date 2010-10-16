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
package com.google.java.contract.tests;

import com.google.java.contract.Ensures;

/**
 * A dummy class overriden in {@link SeparateGenericSuperclassTest}.
 * It has some type parameter with a complex erasure.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
abstract class SeparateGenericSuperclass<T extends Number & Comparable> {
  @Ensures({
    "result != null",
    "result.equals(x)"
  })
  public abstract T f(int x);
}
