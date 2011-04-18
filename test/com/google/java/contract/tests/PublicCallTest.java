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

import com.google.java.contract.Invariant;
import com.google.java.contract.InvariantError;

import junit.framework.TestCase;

/**
 * Tests contracts on public methods for reentry.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class PublicCallTest extends TestCase {
  @Invariant("x != 0")
  private static class SimpleObject {
    public int x;

    public SimpleObject() {
      x = 1;
    }

    public void f() {
    }

    public void g() {
      x = 0;
      f();
      x = 1;
    }

    public void f1() {
      x = 0;
    }

    public void g1() {
      f1();
      x = 1;
    }
  }

  @Invariant("x != 0")
  private static class A {
    public int x;
  }

  private static class B extends A {
    public B() {
      x = 1;
    }
  }

  protected SimpleObject sample;

  @Override
  protected void setUp() {
    sample = new SimpleObject();
  }

  public void testF() {
    sample.x = 1;
    sample.f();
  }

  public void testG() {
    sample.x = 1;
    sample.g();
  }

  public void testF1() {
    try {
      sample.x = 1;
      sample.f1();
      fail();
    } catch (InvariantError expected) {
      assertEquals("[x != 0]", expected.getMessages().toString());
    }
  }

  public void testG1() {
    sample.x = 1;
    sample.g1();
  }

  public void testA() {
    try {
      A a = new A();
      fail();
    } catch (InvariantError expected) {
      assertEquals("[x != 0]", expected.getMessages().toString());
    }
  }

  public void testB() {
    B b = new B();
  }
}
