/*
 * Copyright 2011 Nhat Minh Lê
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
 * Tests contracts with classes that use variadic methods.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class VariadicTest extends TestCase {
  private static class V {
    @Requires("xs.length >= 2")
    public int sub(int... xs) {
      int acc = xs[0];
      for (int i = 1; i < xs.length; ++i) {
        acc -= xs[i];
      }
      return acc;
    }

    @Ensures("xs.length < 2 || result == sub(xs)")
    public int subOrNeg(int... xs) {
      switch (xs.length) {
        case 0:
          return 0;
        case 1:
          return -xs[0];
        default:
          int acc = xs[0];
          for (int i = 1; i < xs.length; ++i) {
            acc -= xs[i];
          }
          return acc;
      }
    }

    public int addMultiSub(int[]... xss) {
      int acc = 0;
      for (int i = 0; i < xss.length; ++i) {
        acc += sub(xss[i]);
      }
      return acc;
    }

    public boolean testSubs(int... xs) {
      return xs.length < 2 ? true : sub(xs) == subOrNeg(xs);
    }

    @Requires("testSubs(1, 2, 3, 4, 5, 6)")
    public void variadicInContract() {
    }

    @Requires("addMultiSub(new int[] { 3, 2, 1 }, new int[] { 1, 2, 3 }) == -4")
    public void multiSubVariadicInContract() {
    }

    @Requires("false")
    public void buggyVariadic(int... xs) {
    }
  }

  private V v;

  @Override
  public void setUp() {
    v = new V();
  }

  public void testSub() {
    assertEquals(v.sub(3, 2, 1), 0);
  }

  public void testSubOrNeg() {
    assertEquals(v.subOrNeg(3, 2, 1), 0);
    assertEquals(v.subOrNeg(4), -4);
  }

  public void testTestSubs() {
    assertTrue(v.testSubs(8932, 678, 20122));
  }

  public void testVariadicInContract() {
    v.variadicInContract();
  }

  public void testMultiSubVariadicInContract() {
    v.multiSubVariadicInContract();
  }

  public void testBuggyVariadic() {
    try {
      v.buggyVariadic(8932, 678, 20122);
      fail();
    } catch (PreconditionError e) {
      /* Expected. */
    }
  }
}
