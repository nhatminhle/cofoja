/*
 * Copyright 2011 Google Inc.
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

import com.google.java.contract.InvariantError;

import junit.framework.TestCase;


/**
 * Tests that contracts are found through inheritance when declared as
 * invariants on the superclass.
 *
 * @author chatain@google.com (Leonardo Chatain)
 */
public class SeparateInvariantSuperclassTest extends TestCase{
  private static class SeparateChild extends SeparateInvariantSuperclass {
    @Override
    public void violate() {
      x = 0;
    }
  }

  private SeparateChild child;

  @Override
  protected void setUp() {
    child = new SeparateChild();
  }

  public void testViolate() {
    try {
      child.violate();
      fail();
    } catch (InvariantError expected) {
      /* Bogus implementation. */
    }
  }
}
