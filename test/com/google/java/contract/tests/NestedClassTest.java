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
import com.google.java.contract.PreconditionError;
import com.google.java.contract.Requires;

import junit.framework.TestCase;

/**
 * Tests contracts that apply to a nested class.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class NestedClassTest extends TestCase {
  static class NestedClass {
    @Requires("true")
    public void nestedSuccess() {
    }

    @Requires("false")
    public void nestedFailure() {
    }

    @Requires({ "true", "true && true" })
    @Ensures({ "true || true", "true" })
    public void nestedMultiSuccess() {
    }

    /*
     * This test checks that Contracts for Java is doing its job
     * right when parsing the contract method names that
     * contain '$' and identifies the correct failing
     * predicate.
     */
    @Requires({ "true", "true && true" })
    @Ensures({ "true && false", "true" })
    public void nestedSingleFailure() {
    }

    @Requires("getTrue()")
    public void outerAccessSuccess() {
    }

    @Requires("getFalse()")
    public void outerAccessFailure() {
    }
  }

  private static boolean getTrue() {
    return true;
  }

  private static boolean getFalse() {
    return false;
  }

  NestedClass sample;

  @Override
  protected void setUp() {
    sample = new NestedClass();
  }

  public void testNestedSuccess() {
    sample.nestedSuccess();
  }

  public void testNestedFailure() {
    try {
      sample.nestedFailure();
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[false]", expected.getMessages().toString());
    }
  }

  public void testNestedMultiSuccess() {
    sample.nestedMultiSuccess();
  }

  public void testNestedSingleFailure() {
    try {
      sample.nestedSingleFailure();
      fail();
    } catch (PostconditionError expected) {
      assertEquals("[true && false]", expected.getMessages().toString());
    }
  }

  public void testOuterAccessSuccess() {
    sample.outerAccessSuccess();
  }

  public void testOuterAccessFailure() {
    try {
      sample.outerAccessFailure();
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[getFalse()]", expected.getMessages().toString());
    }
  }
}
