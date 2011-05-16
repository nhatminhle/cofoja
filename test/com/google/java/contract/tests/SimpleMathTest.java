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
import com.google.java.contract.PostconditionError;
import com.google.java.contract.PreconditionError;
import com.google.java.contract.Requires;
import com.google.java.contract.ThrowEnsures;

import junit.framework.TestCase;

/**
 * Tests contracts on some simple mathematical functions. Among other
 * things, these contracts make use of {@code result} and {@code old}.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class SimpleMathTest extends TestCase {
  private static class SimpleMath {
    /*
     * This is just for the test; usually, you should not reference
     * the parameters directly, and use old() instead.
     * TODO(lenh): Should we be able to do that at all?
     */
    @Ensures("result == x + y")
    public static int add(int x, int y) {
      return x + y;
    }

    @Ensures("result == x + y")
    public static int bogusAdd(int x, int y) {
      return x + y + 1;
    }

    @Requires({ "x > 0", "y > 0" })
    @Ensures({
      "result != 0",
      "old (x) % result == 0",
      "old (y) % result == 0"
    })
    public static int gcd(int x, int y) {
      while (x != 0 && y != 0) {
        if (x > y) {
            x -= y;
        } else {
            y -= x;
        }
      }

      return (x != 0) ? x : y;
    }

    @Requires({ "x > 0", "y > 0" })
    @Ensures({
      "result != 0",
      "old (x) % result == 0",
      "old (y) % result == 0"
    })
    public static int bogusGcd(int x, int y) {
      while (x != 0 && y != 0) {
        if (x > y) {
            x -= y;
        } else {
            y -= x;
        }
      }

      return (x != 0) ? y : x;
    }

    @Requires({ "x > 0", "y > 0" })
    @Ensures({
      "result != 0",
      "old (x) % result == 0",
      "old (y) % result == 0"
    })
    public static int bogusGcd1(int x, int y) {
      return x;
    }

    @Requires("n >= 0")
    @Ensures("!result || old (n) % 2 == 0")
    public static boolean even(int n) {
      if (n == 0) {
        return true;
      } else {
        return odd(n - 1);
      }
    }

    @Requires("n >= 0")
    @Ensures({
      "!result || old (n) % 2 == 1",
      /* For testing purposes: check that old () has the right value. */
      "old (n) == n"
    })
    public static boolean odd(int n) {
      if (n == 0) {
        return false;
      } else {
        return even(n - 1);
      }
    }

    @ThrowEnsures({ "IllegalArgumentException", "old (x) < 0" })
    public static double sqrt(double x) {
      if (x < 0) {
        throw new IllegalArgumentException();
      }
      return Math.sqrt(x);
    }

    @ThrowEnsures({ "IllegalArgumentException", "old (x) < -1" })
    public static double bogusSqrt(double x) {
      if (x < 0) {
        throw new IllegalArgumentException();
      }
      return Math.sqrt(x);
    }
  }

  public void testAdd() {
    assertEquals(SimpleMath.add(1, 2), 3);
  }

  public void testBogusAdd() {
    try {
      SimpleMath.bogusAdd(1, 2);
      fail();
    } catch (PostconditionError expected) {
      assertEquals("[result == x + y]", expected.getMessages().toString());
    }
  }

  public void testGcd() {
    assertEquals(SimpleMath.gcd(83295, 37285), 5);
  }

  public void testGcdInvalidArguments() {
    try {
      SimpleMath.gcd(-1382, -3287);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[x > 0]", expected.getMessages().toString());
    }
  }

  public void testBogusGcd() {
    try {
      SimpleMath.bogusGcd(83295, 37285);
      fail();
    } catch (PostconditionError expected) {
      assertEquals("[result != 0]", expected.getMessages().toString());
    }
  }

  public void testBogusGcd1() {
    try {
      SimpleMath.bogusGcd1(83295, 37285);
      fail();
    } catch (PostconditionError expected) {
      assertEquals("[old (y) % result == 0]", expected.getMessages().toString());
    }
  }

  public void testEven() {
    assertEquals(SimpleMath.even(392), true);
  }

  public void testSqrt() {
    SimpleMath.sqrt(4);
  }

  public void testSqrtInvalidArgument() {
    try {
      SimpleMath.sqrt(-3);
      fail();
    } catch (IllegalArgumentException expected) {
      /* Expected to pass through the contract. */
    }
  }

  public void testBogusSqrt() {
    try {
      SimpleMath.bogusSqrt(-0.5);
      fail();
    } catch (PostconditionError expected) {
      assertEquals(expected.getMessages().toString(),
                   "[IllegalArgumentException => old (x) < -1]");
    }
  }
}
