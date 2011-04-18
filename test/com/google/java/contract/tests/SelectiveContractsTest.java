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

import com.google.java.contract.ContractEnvironment;
import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.InvariantError;
import com.google.java.contract.PostconditionError;
import com.google.java.contract.PreconditionError;
import com.google.java.contract.Requires;

import junit.framework.TestCase;

/**
 * Tests selective contract activation.
 *
 * @see Cofoja
 */
public class SelectiveContractsTest extends TestCase {
  private static class A {
    @Requires("false")
    public static void f() {
    }
  }

  private static class B {
    @Ensures("false")
    public static void f() {
    }
  }

  @Invariant("false")
  private static class C {
  }

  private static class D {
    @Requires("false")
    @Ensures("false")
    public static void f() {
    }
  }

  @Invariant("false")
  private static class E {
    @Requires("false")
    @Ensures("false")
    public void f() {
    }
  }

  @Invariant("false")
  private static class F {
    @Requires("false")
    public void f() {
    }
  }

  @Invariant("false")
  private static interface I {
  }

  /* Missing @Contracted. */
  private static class G implements I {
  }

  @Override
  protected void setUp() {
    ContractEnvironment env = Cofoja.contractEnv;

    env.disablePreconditions("com.google.java.contract.tests.SelectiveContractsTest$A");
    env.disablePostconditions("com.google.java.contract.tests.SelectiveContractsTest$B");
    env.disableInvariants("com.google.java.contract.tests.SelectiveContractsTest$C");
    env.disablePreconditions("com.google.java.contract.tests.SelectiveContractsTest$D");
    env.disableInvariants("com.google.java.contract.tests.SelectiveContractsTest$E");

    env.ignore("com.google.java.contract.tests.SelectiveContractsTest$F");
    env.ignore("com.google.java.contract.tests.SelectiveContractsTest$G");

    env.disableInvariants("com.google.java.contract.tests.selective.a.*");
    env.disableInvariants("com.google.java.contract.tests.selective.b.*");
    env.enableInvariants("com.google.java.contract.tests.selective.b.y.*");
    env.enableInvariants("com.google.java.contract.tests.selective.a.x.X1");
  }

  public void testA() {
    A.f();
  }

  public void testB() {
    B.f();
  }

  public void testC() {
    new C();
  }

  public void testD() {
    try {
      D.f();
      fail();
    } catch (PostconditionError expected) {
      /* Expected since the precondition should be disabled. */
    }
  }

  public void testE() {
    try {
      new E().f();
      fail();
    } catch (PreconditionError expected) {
      /* Expected since the invariant should be disabled. */
    }
  }

  public void testF() {
    new F().f();
  }

  public void testG() {
    new G();
  }

  public void testStarPattern() {
    new com.google.java.contract.tests.selective.a.A();
    new com.google.java.contract.tests.selective.a.x.X();
  }

  public void testOverridePattern() {
    try {
      new com.google.java.contract.tests.selective.a.x.X1();
    } catch (InvariantError expected) {
      /* Expected since the star pattern should be overriden. */
    }
  }

  public void testOverrideStarPattern() {
    new com.google.java.contract.tests.selective.b.B();
    try {
      new com.google.java.contract.tests.selective.b.y.Y();
    } catch (InvariantError expected) {
      /* Expected since the first star pattern should be overriden. */
    }
  }
}
