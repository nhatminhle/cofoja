/*
 * Copyright 2007 Johannes Rieken
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

import com.google.java.contract.Requires;

/**
 * A dummy interface overriden in {@link SeparateInterfaceTest}.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
interface SeparateInterface {
  @Requires("x >= 0")
  public void f(int x);

  @Requires("x < y")
  public void g(int x, int y);

  @Requires("x < y + z")
  public void h(int x, int y, int z);

  @Requires("x > y")
  public void k(int x, int y);
}
