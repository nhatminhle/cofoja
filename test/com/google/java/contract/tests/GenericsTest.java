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
import com.google.java.contract.PreconditionError;
import com.google.java.contract.Requires;
import junit.framework.TestCase;

/**
 * Tests contracts that apply to generic classes and interfaces.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class GenericsTest extends TestCase {
  @Invariant("a.length() >= 2")
  private static class A<T> {
    protected String a;

    public A(T x) {
      /* Bogus for some values of x (for example, ""). */
      a = x.toString();
    }
  }

  private static class B extends A<Integer> {
    /*
     * Note: the super call is always the *first* thing to be executed
     * in a constructor; it also means the call is made before any
     * preconditions. In this case, the invariant of class A gets
     * executed before the precondition of the constructor of B. This
     * is a counter-intuitive.
     */
    @Requires("x >= 9")
    public B(int x) {
      super(x);
    }
  }

  private static class S<T> {
    private T x;

    public S(T x) {
      this.x = x;
    }

    @Override
    public String toString() {
      return x.toString();
    }
  }

  private static class C<U extends S> extends A<U> {
    public C(U u) {
      super(u);
    }
  }

  private static class D<T> {
    private A<S<T>> a;

    public D(T x) {
      a = new C<S<T>>(new S<T>(x));
    }
  }

  private static interface I {
    @Requires("x.toString().length() >= 1")
    @Ensures("result >= 1")
    public <T> int f(T x);
  }

  private static class E implements I {
    @Override
    public <T> int f(T x) {
      return x.toString().length();
    }
  }

  private static interface J<T> {
    @Requires("x.toString().length() >= 2")
    @Ensures("result >= 2")
    public int g(T x);
  }

  private static class F implements J<Integer> {
    @Override
    public int g(Integer x) {
      return x.toString().length();
    }
  }

  private static class TT<X extends Throwable> {
    /* Test ability to contract methods that throw type parameters. */
    @Requires("true")
    public void f(X x) throws X {
      throw x;
    }
  }

  private static class Npe extends TT<NullPointerException> {
    @Override
    @Ensures("true")
    public void f(NullPointerException x) throws NullPointerException {
      throw x;
    }
  }

  public void testB() {
    B b = new B(48);
  }

  public void testBBogus() {
    try {
      B b = new B(9);
      fail();
    } catch (InvariantError expected) {
      assertEquals("[a.length() >= 2]", expected.getMessages().toString());
    }
  }

  public void testC() {
    C<S<Integer>> c = new C<S<Integer>>(new S<Integer>(283));
  }

  public void testCBogus() {
    try {
      C<S<Integer>> c = new C<S<Integer>>(new S<Integer>(3));
      fail();
    } catch (InvariantError expected) {
      assertEquals("[a.length() >= 2]", expected.getMessages().toString());
    }
  }

  public void testD() {
    D<Integer> d = new D<Integer>(new Integer(7892));
  }

  public void testDBogus() {
    try {
      D<Integer> d = new D<Integer>(new Integer(4));
      fail();
    } catch (InvariantError expected) {
      assertEquals("[a.length() >= 2]", expected.getMessages().toString());
    }
  }

  public void testE() {
    E e = new E();
    e.f(23);
  }

  public void testEBogus() {
    try {
      E e = new E();
      e.f("");
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[x.toString().length() >= 1]", expected.getMessages().toString());
    }
  }

  public void testF() {
    F f = new F();
    f.g(79);
  }

  public void testFBogus() {
    try {
      F f = new F();
      f.g(1);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[x.toString().length() >= 2]", expected.getMessages().toString());
    }
  }

  public void testT() {
    Npe npe = new Npe();
    try {
      npe.f(new NullPointerException());
      fail();
    } catch (NullPointerException e) {
      /* Expected exception. */
    }
  }
}
