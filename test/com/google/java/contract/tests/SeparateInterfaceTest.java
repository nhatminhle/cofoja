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

import com.google.java.contract.PreconditionError;
import junit.framework.TestCase;

/**
 * Tests renaming of interface method parameters.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class SeparateInterfaceTest extends TestCase {
  private static class SeparateImplementation implements SeparateInterface {
    @Override
    public void f(int y) {
    }

    @Override
    public void g(int y, int x) {
    }

    @Override
    public void h(int y, int z, int x) {
    }

    @Override
    public void k(int x, int y) {
    }
  }

  private SeparateImplementation sample;

  @Override
  protected void setUp() {
    sample = new SeparateImplementation();
  }

  public void testF() {
    sample.f(846);
  }

  public void testFIllegalArgument() {
    try {
      sample.f(-67);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[x >= 0]", expected.getMessages().toString());
    }
  }

  public void testG() {
    sample.g(28, 371);
  }

  public void testGIllegalArgument() {
    try {
      sample.g(2485, 846);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[x < y]", expected.getMessages().toString());
    }
  }

  public void testH() {
    sample.h(2, 1, 3);
  }

  public void testHIllegalArgument() {
    try {
      sample.h(3, 1, 2);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[x < y + z]", expected.getMessages().toString());
    }
  }

  public void testK() {
    sample.k(81, 67);
  }

  public void testKIllegalArgument() {
    try {
      sample.k(67, 81);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[x > y]", expected.getMessages().toString());
    }
  }
}
