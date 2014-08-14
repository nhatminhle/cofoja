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

import com.google.java.contract.Invariant;
import com.google.java.contract.InvariantError;
import com.google.java.contract.PreconditionError;
import com.google.java.contract.Requires;
import junit.framework.TestCase;

import java.io.FileNotFoundException;

/**
 * Tests contracts that apply to constructors.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class ConstructorTest extends TestCase {
  @Invariant("a >= 0")
  private static class A {
    protected int a;

    public A(int x) {
      /* Bogus for x < 0. */
      a = x;
    }
  }

  private static class B extends A {
    public B(int x) {
      /* Bogus for x < 0. */
      super(x);
    }
  }

  private static class C extends A {
    @Requires("x >= 1")
    public C(int x) {
      super(x);
    }
  }

  /* double super argument. */
  private static class D {
    @Requires("x >= 0")
    public D(double x) {
    }
  }

  private static class Dx extends D {
    public Dx(double x) {
      super(x);
    }
  }

  /* char super argument. */
  private static class Ch {
    @Requires("x != '\0'")
    public Ch(char x) {
    }
  }

  private static class Chx extends Ch {
    public Chx(char x) {
      super(x);
    }
  }

  private static class In {
    @Requires("!\"/dev/null\".equals(name)")
    public In(String name) throws FileNotFoundException {
      if (!"/dev/null".equals(name))
        throw new FileNotFoundException(name);
    }
  }

  public void testA() {
    A b = new A(84);
  }

  public void testABogus() {
    try {
      A b = new A(-64390);
      fail();
    } catch (InvariantError expected) {
      assertEquals("[a >= 0]", expected.getMessages().toString());
    }
  }

  public void testB() {
    B b = new B(1846);
  }

  public void testBBogus() {
    try {
      B b = new B(-3829);
      fail();
    } catch (InvariantError expected) {
      assertEquals("[a >= 0]", expected.getMessages().toString());
    }
  }

  public void testCInvalidArgument() {
    try {
      C b = new C(0);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[x >= 1]", expected.getMessages().toString());
    }
  }

  public void testDx() {
    try {
      Dx dx = new Dx(-3.0);
    } catch (PreconditionError expected) {
      assertEquals("[x >= 0]", expected.getMessages().toString());
    }
  }

  public void testChx() {
    try {
      Chx chx = new Chx('\0');
    } catch (PreconditionError expected) {
      assertEquals("[x != '\0']", expected.getMessages().toString());
    }
  }

  public void testIn() {
    try {
      In in = new In("XXX");
      fail();
    } catch (FileNotFoundException expected) {
      /* File 'XXX' does not exist. */
    }
  }

  public void testInInvalidArgument() throws FileNotFoundException {
    try {
      In in = new In("/dev/null");
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[!\"/dev/null\".equals(name)]", expected.getMessages().toString());
    }
  }
}
