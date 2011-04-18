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
import com.google.java.contract.Invariant;
import com.google.java.contract.InvariantError;
import com.google.java.contract.PostconditionError;
import com.google.java.contract.PreconditionError;
import com.google.java.contract.Requires;
import junit.framework.TestCase;

/**
 * Tests contract inheritance through superclasses and interfaces.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@SuppressWarnings("unused")
public class InheritanceTest extends TestCase {
  @Invariant("a >= 0")
  private abstract static class A {
    private int p;
    protected int a;

    @Requires("x >= 0")
    public int f(int x) {
      a = x;
      return x;
    }

    @Requires("x == 1")
    @Ensures("result >= old (x)")
    public abstract int g(int x);

    @Ensures("p == old (p)")
    public void q() {
    }
  }

  @Invariant("b >= 0 || b == -1")
  private static class B extends A {
    protected int b;

    @Override
    @Requires("x == -1")
    @Ensures("result >= old (x)")
    public int f(int x) {
      /* Bogus for x == -1. */
      a = b = x;
      return x;
    }

    @Override
    public int g(int x) {
      return x + 1;
    }

    /*
     * Tests that private fields are accessible from within old-value
     * expressions even when inherited.
     */
    @Override
    public void q() {
    }
  }

  private static class C extends B {
    @Override
    @Requires("x >= 1")
    public int g(int x) {
      /* Bogus. */
      return x - 1;
    }
  }

  @Invariant("k() == 1")
  private static interface I {
    int k();

    @Requires("y > x")
    @Ensures("result > 0")
    int h(int x, int y);
  }

  private abstract static class J implements I {
    @Override
    public int k() {
      return 1;
    }

    @Override
    public int h(int x, int y) {
      return y - x;
    }
  }

  private static class D extends B implements I {
    /* Necessary so that invariants will apply. */
    public D() {
    }

    @Override
    public int k() {
      return 1;
    }

    @Override
    public int h(int x, int y) {
      return y - x;
    }
  }

  private static class E extends B implements I {
    /* Necessary so that invariants will apply. */
    public E() {
    }

    @Override
    public int k() {
      /* Bogus. */
      return 2;
    }

    @Override
    public int h(int x, int y) {
      return y - x;
    }
  }

  private static class F extends B implements I {
    /* Necessary so that invariants will apply. */
    public F() {
    }

    @Override
    public int k() {
      return 1;
    }

    @Override
    @Requires("y >= x")
    public int h(int x, int y) {
      /* Bogus for x == y. */
      return y - x;
    }
  }

  /*
   * Complicated way to get h() through an interface call.
   */
  private static int getK(I obj) {
    return obj.k();
  }

  @Invariant("getK(this) == 1")
  private static class G extends J {
    /* Necessary so that invariants will apply. */
    public G() {
    }
  }

  @Invariant("getK(this) == 2")
  private static class H extends J {
    /* Necessary so that invariants will apply. */
    public H() {
    }
  }

  B b;
  C c;

  D d;
  F f;

  @Override
  protected void setUp() {

    b = new B();
    c = new C();

    d = new D();
    f = new F();
  }

  public void testBF() {
    assertEquals(b.f(42), 42);
  }

  public void testBFInvalidArgument() {
    try {
      b.f(-36);
      fail();
    } catch (PreconditionError expected) {
      /*
       * TODO(lenh): Ordering of the messages is not predictable at
       * the moment; the exception types should be extended with a
       * getter to return the message array instead of just a message.
       */
      assertEquals("[x >= 0, x == -1]", expected.getMessages().toString());
    }
  }

  public void testBFBogusArgument() {
    try {
      b.f(-1);
      fail();
    } catch (InvariantError expected) {
      assertEquals("[a >= 0]", expected.getMessages().toString());
    }
  }

  public void testBG() {
    assertEquals(b.g(1), 2);
  }

  public void testBGInvalidArgument() {
    try {
      b.g(3);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[x == 1]", expected.getMessages().toString());
    }
  }

  public void testBQ() {
    b.q();
  }

  public void testCGBogus() {
    try {
      c.g(735);
      fail();
    } catch (PostconditionError expected) {
      assertEquals("[result >= old (x)]", expected.getMessages().toString());
    }
  }

  public void testDFBogusArgument() {
    try {
      d.f(-1);
      fail();
    } catch (InvariantError expected) {
      assertEquals("[a >= 0]", expected.getMessages().toString());
    }
  }

  public void testDH() {
    assertEquals(d.h(37, 56), 19);
  }

  public void testEBogusInvariant() {
    try {
      E e = new E();
      fail();
    } catch (InvariantError expected) {
      assertEquals("[k() == 1]", expected.getMessages().toString());
    }
  }

  public void testFHBogusArgument() {
    try {
      f.h(328, 328);
      fail();
    } catch (PostconditionError expected) {
      assertEquals("[result > 0]", expected.getMessages().toString());
    }
  }

  public void testG() {
    G g = new G();
  }

  public void testH() {
    try {
      H h = new H();
      fail();
    } catch (InvariantError expected) {
      assertEquals("[getK(this) == 2]", expected.getMessages().toString());
    }
  }
}
