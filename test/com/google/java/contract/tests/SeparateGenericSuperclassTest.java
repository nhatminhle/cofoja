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

import static junit.framework.Assert.fail;

import com.google.java.contract.Contracted;
import com.google.java.contract.PostconditionError;
import junit.framework.TestCase;

/**
 * Tests inheritance of postconditions of methods returning a generic
 * type parameter with complex erasure in a superclass compiled
 * separately.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class SeparateGenericSuperclassTest extends TestCase {
  @Contracted
  private static class SeparateChild
      extends SeparateGenericSuperclass<Integer> {
    @Override
    public Integer f(int x) {
      return x + 1;
    }
  }

  protected void setUp() {
    Cofoja.contractEnv.assertLoadedClassesContracted();
  }

  public void testF() {
    try {
      new SeparateChild().f(6379);
      fail();
    } catch (PostconditionError expected) {
      /* Bogus implementation. */
    }
  }
}
