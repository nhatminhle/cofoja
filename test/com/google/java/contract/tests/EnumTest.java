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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import com.google.java.contract.PreconditionError;
import com.google.java.contract.Requires;
import junit.framework.TestCase;

/**
 * Tests contracts that apply to enum classes.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class EnumTest extends TestCase {
  private static enum E {
    X, Y, Z;

    @Requires("this != Z")
    public int f() {
      return this == X ? 1 : 2;
    }
  }

  private static enum F {
    X(1), Y(2), Z(3);

    private F(int x) {
    }

    @Requires("this != Z")
    public int f() {
      return this == X ? 1 : 2;
    }
  }

  protected void setUp() {
    Cofoja.contractEnv.assertLoadedClassesContracted();
  }

  public void testX() {
    E e = E.X;
    e.f();
  }

  public void testZ() {
    E e = E.Z;
  }

  public void testZIllegalState() {
    E e = E.Z;
    try {
      e.f();
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[this != Z]", expected.getMessages().toString());
    }
  }
}
