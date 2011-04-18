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

import junit.framework.TestCase;

import java.io.Serializable;

/**
 * Tests the correct typing of result variables through contract
 * inheritance.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@SuppressWarnings("unused")
public class ReturnTypeTest extends TestCase {
  private static class A {
    @Ensures({
      "result != null",
      "result.equals(x)"
    })
    public Number f(int x) {
      return x;
    }
  }

  private static class B extends A {
    /* Covariant return type. */
    @Override
    public Integer f(int x) {
      /* Bogus. */
      return x + 1;
    }
  }

  private static interface I<T> {
    @Ensures({
      "result != null",
      "result.equals(x)"
    })
    public T f(int x);
  }

  private static class C implements I<Integer> {
    /* Specialized return type. */
    @Override
    public Integer f(int x) {
      /* Bogus. */
      return x - 1;
    }
  }

  private static class C1 implements I<Number> {
    /* Specialized and covariant return type. */
    @Override
    public Integer f(int x) {
      /* Bogus. */
      return x - 1;
    }
  }

  private static class C2 extends A implements I<Comparable> {
    /* Specialized and doubly covariant return type. */
    @Override
    public Integer f(int x) {
      /* Bogus. */
      return x - 1;
    }
  }

  /* Complex erasure from a superclass. */
  private abstract static class J<T extends Serializable & Comparable> {
    @Ensures({
      "result != null",
      "result.compareTo(x) == 0"
    })
    public abstract T f(int x);
  }

  private static class D extends J<Integer> {
    /* Specialized return type. */
    @Override
    public Integer f(int x) {
      /* Bogus. */
      return x - 1;
    }
  }

  public void testB() {
    try {
      new B().f(8326);
      fail();
    } catch (PostconditionError expected) {
      /* Bogus implementation. */
    }
  }

  public void testC() {
    try {
      new C().f(584);
      fail();
    } catch (PostconditionError expected) {
      /* Bogus implementation. */
    }
  }

  public void testC1() {
    try {
      new C1().f(206);
      fail();
    } catch (PostconditionError expected) {
      /* Bogus implementation. */
    }
  }

  public void testC2() {
    try {
      new C2().f(4896);
      fail();
    } catch (PostconditionError expected) {
      /* Bogus implementation. */
    }
  }

  public void testD() {
    try {
      new D().f(9075);
      fail();
    } catch (PostconditionError expected) {
      /* Bogus implementation. */
    }
  }
}
