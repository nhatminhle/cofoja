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
import com.google.java.contract.PreconditionError;
import com.google.java.contract.Requires;

import junit.framework.TestCase;

/**
 * Tests implies operator syntax.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class MiscSyntaxTest extends TestCase {
  @Ensures("result == (x => y)")
  private static boolean implies(boolean x, boolean y) {
    return !x || y;
  }

  @Requires("a => (b || (c && d => e == f))")
  private static void f(boolean a, boolean b, boolean c,
                        boolean d, boolean e, boolean f) {
  }

  @Requires("(a => b => c) => d")
  private static void g(boolean a, boolean b, boolean c, boolean d) {
  }

  public void testImplies() {
    implies(false, false);
    implies(false, true);
    implies(true, false);
    implies(true, true);
  }

  public void testFSuccess() {
    f(true, false, true, true, false, false);
  }

  public void testFSuccess1() {
    f(true, false, false, true, true, false);
  }

  public void testFSuccess2() {
    f(true, true, true, true, true, false);
  }

  public void testFFailure() {
    try {
      f(true, false, true, true, true, false);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[a => (b || (c && d => e == f))]",
                   expected.getMessages().toString());
    }
  }

  public void testFFailure1() {
    try {
      f(true, false, true, true, false, true);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[a => (b || (c && d => e == f))]",
                   expected.getMessages().toString());
    }
  }

  public void testGSuccess() {
    g(false, false, false, true);
  }

  public void testGSuccess1() {
    g(true, false, false, true);
  }

  public void testGSuccess2() {
    g(true, true, false, false);
  }

  public void testGFailure() {
    try {
      g(false, false, false, false);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[(a => b => c) => d]",
                   expected.getMessages().toString());
    }
  }

  public void testGFailure1() {
    try {
      g(true, true, true, false);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[(a => b => c) => d]",
                   expected.getMessages().toString());
    }
  }
}
