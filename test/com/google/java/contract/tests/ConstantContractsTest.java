/*
 * Copyright 2007 Johannes Rieken
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

import com.google.java.contract.PostconditionError;
import com.google.java.contract.PreconditionError;

import junit.framework.TestCase;

/**
 * Tests contracts made of constant expressions.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class ConstantContractsTest extends TestCase {
  ConstantContracts sample;

  @Override
  protected void setUp() {
    sample = new ConstantContracts();
  }

  public void testPreFailsAndHasCorrectMessage() {
    try {
      sample.preFailure();
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[false || false]", expected.getMessages().toString());
    }
  }

  public void testPostFailure() {
    try {
      sample.postFailure();
      fail();
    } catch (PostconditionError expected) {
      /* Expected. */
    }
  }

  public void testPostFailure1() {
    try {
      sample.postFailure1();
      fail();
    } catch (PostconditionError expected) {
      /* Expected. */
    }
  }

  public void testPostFailure2() {
    try {
      sample.postFailure2();
      fail();
    } catch (PostconditionError expected) {
      /* Expected. */
    }
  }

  public void testPreSuccess() {
    sample.preSuccess();
  }

  public void testPostSuccess() {
    sample.postSuccess();
  }

  public void testPostSuccess1() {
    sample.postSuccess1();
  }

  public void testPostSuccess2() {
    sample.postSuccess2();
  }

  public void testPostSuccess3() {
    sample.postSuccess3();
  }

  public void testOldSuccess() {
    sample.oldSuccess();
  }

  public void testOldSuccess1() {
    sample.oldSuccess1();
  }

  public void testOldSuccess2() {
    sample.oldSuccess2();
  }

  public void testOldSuccess3() {
    try {
      sample.oldSuccess3();
      fail();
    } catch (RuntimeException expected) {
      /* Expected. */
    }
  }

  public void testOldFailure() {
    try {
      sample.oldFailure();
      fail();
    } catch (PostconditionError expected) {
      /* Expected. */
    }
  }
}
