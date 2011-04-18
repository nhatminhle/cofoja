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
 * Tests simple contracts that apply to class members. This test case
 * does not include member contracts that depend on special features
 * such as {@code old}, {@code result} or {@code signal}.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class MemberContractsTest extends TestCase {
  @Invariant("count >= 0")
  static class MemberContracts {
    private int count;

    public MemberContracts() {
      count = 0;
    }

    @Requires("n >= 0")
    public int naturalIdentity(int n) {
      return n;
    }

    /*
     * Used to check the internal value, to make sure
     * contracts do not alter normal behavior.
     */
    public int getCount() {
      return count;
    }

    public void add(int n) {
      count += n;
    }

    @Requires("n >= 1")
    @Ensures("count >= 1")
    public void addPositive(int n) {
      count += n;
    }

    @Requires("n >= 1")
    @Ensures("count >= 1")
    public void addPositiveBogus(int n) {
      /* Error on purpose. */
      count += n - 1;
    }

    public static boolean allNatural(int[] array) {
      for (int n : array) {
        if (n < 0) {
          return false;
        }
      }
      return true;
    }

    @Requires("allNatural(array)")
    public void addNaturals(int... array) {
      for (int n : array) {
        add(n);
      }
    }
  }

  MemberContracts sample;

  @Override
  protected void setUp() {
    sample = new MemberContracts();
  }

  public void testIdentitySuccess() {
    assertEquals(sample.naturalIdentity(42), 42);
  }

  public void testIdentityFailure() {
    try {
      sample.naturalIdentity(-36);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[n >= 0]", expected.getMessages().toString());
    }
  }

  public void testInvariantSuccess() {
    sample.add(71);
    assertEquals(sample.getCount(), 71);
  }

  public void testInvariantFailure() {
    try {
      sample.add(-12);
      fail();
    } catch (InvariantError expected) {
      assertEquals("[count >= 0]", expected.getMessages().toString());
    }
  }

  public void testAddChainFailure() {
    try {
      sample.add(234);
      sample.add(-123);
      sample.add(-200);
      fail();
    } catch (InvariantError expected) {
      assertEquals("[count >= 0]", expected.getMessages().toString());
    }
  }

  public void testAddPositive() {
    sample.addPositive(42);
    assertEquals(sample.getCount(), 42);
  }

  public void testAddPositiveBogusSuccess() {
    sample.addPositiveBogus(42);
  }

  public void testAddPositiveBogusFailure() {
    try {
      sample.addPositiveBogus(1);
      fail();
    } catch (PostconditionError expected) {
      assertEquals("[count >= 1]", expected.getMessages().toString());
    }
  }

  public void testAddNaturalsSuccess() {
    sample.addNaturals(1, 2, 3, 4);
    assertEquals(sample.getCount(), 10);
  }

  public void testAddNaturalsFailure() {
    try {
      sample.addNaturals(1, 2, 3, -4, 5);
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[allNatural(array)]", expected.getMessages().toString());
    }
  }
}
