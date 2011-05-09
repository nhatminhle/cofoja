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

import com.google.java.contract.Invariant;
import com.google.java.contract.InvariantError;
import com.google.java.contract.PreconditionError;
import com.google.java.contract.Requires;

import junit.framework.TestCase;

/**
 * Tests that invariants are correctly applied to final fields, which can
 * be initialized during the constructor call or before.
 *
 * @author chatain@google.com (Leonardo Chatain)
 */
public class FinalFieldsTest extends TestCase {
  private static interface FinalFieldInterface {
    public final int FIELD = 42;
    @Requires("FIELD == 42")
    public void success();
    @Requires("FIELD != 42")
    public void fail();
  }

  public void testFinalField() {
    @Invariant("field == 1")
    class ContractedFinalField {
      private final int field;

      public ContractedFinalField(int field) {
        this.field = field;
      }
    }

    ContractedFinalField cff = new ContractedFinalField(1);
    try {
      new ContractedFinalField(0);
    } catch (InvariantError e) {
      /* Bogus implementation. */
    }
  }

  public void testContractedConstant() {
    @Invariant("FIELD == 1")
    class ContractedConstant {
      private static final int FIELD = 1;

      @Requires("FIELD != 1")
      public void fail() {
        /* The contract of this method causes it to fail. */
      }
    }

    ContractedConstant cc = new ContractedConstant();
    try {
      cc.fail();
    } catch(PreconditionError e) {
      /* Bogus implementation. */
    }
  }

  public void testContractedFinalDefaultValue() {
    @Invariant("field == 1")
    class ContractedFinalDefautValue {
      private final int field = 1;

      @Requires("field != 1")
      public void fail() {
        /* The contract of this method causes it to fail. */
      }
    }

    ContractedFinalDefautValue cfdv = new ContractedFinalDefautValue();
    try {
      cfdv.fail();
    } catch (PreconditionError e) {
      /* Bogus implementation. */
    }
  }

  public void testFinalInterface() {
    class Implementor implements FinalFieldInterface {
      @Override
      public void success() {
        /* The contract of this method should not throw an exception. */
      }
      @Override
      public void fail() {
        /* The contract of this method causes it to fail. */
      }
    }

    Implementor implementor = new Implementor();
    implementor.success();
    try {
      implementor.fail();
    } catch (PreconditionError e) {
      /* Bogus implementation. */
    }
  }
}
