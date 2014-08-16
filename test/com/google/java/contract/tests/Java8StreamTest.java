/*
 * Copyright 2014 Nhat Minh Lê
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Tests Java 8 lambdas on collections in contracts.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class Java8StreamTest extends TestCase {
  @Invariant("xs.stream().allMatch(x -> x >= 0)")
  private static class A {
    protected List<Integer> xs;
    private int k;

    public A(Collection<Integer> xs, int k) {
      /* Bogus if xs contains negative elements. */
      this.xs = new ArrayList(xs);
      this.k = k;
    }

    @Requires("xs.stream().reduce(0, (x, y) -> x + y) % 2 == 0")
    public static boolean onlyEven(Collection<Integer> xs) {
      return true;
    }

    @Requires("xs.stream().reduce(0, (x, y) -> k + x + y) % 2 == 0")
    public boolean instanceCapture() {
      return true;
    }
  }

  public void testA() {
    A b = new A(Arrays.asList(1, 2, 3, 4), 42);
  }

  public void testABogus() {
    try {
      A b = new A(Arrays.asList(1, 2, 3, -4), 42);
    } catch (InvariantError expected) {
      assertEquals("[xs.stream().allMatch(x -> x >= 0)]",
                   expected.getMessages().toString());
    }
  }

  public void testOnlyEven() {
    boolean b = A.onlyEven(Arrays.asList(2, 3, 3));
  }

  public void testOnlyEvenInvalid() {
    try {
      boolean b = A.onlyEven(Arrays.asList(2, 3, 4));
    } catch (PreconditionError expected) {
      assertEquals("[xs.stream().reduce(0, (x, y) -> x + y) % 2 == 0]",
                   expected.getMessages().toString());
    }
  }

  public void testInstanceCapture() {
    boolean b = new A(Arrays.asList(2, 3, 4), 11).instanceCapture();
  }

  public void testInstanceCaptureInvalid() {
    try {
      boolean b = new A(Arrays.asList(2, 3, 3), 11).instanceCapture();
    } catch (PreconditionError expected) {
      assertEquals("[xs.stream().reduce(0, (x, y) -> k + x + y) % 2 == 0]",
                   expected.getMessages().toString());
    }
  }
}
