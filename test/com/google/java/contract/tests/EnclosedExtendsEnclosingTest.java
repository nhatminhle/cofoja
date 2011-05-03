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
 * Tests that ContractFinder finds contracts on when an enclosed class extends
 * the enclosing one.
 *
 * @author chatain@google.com (Leonardo Chatain)
 */
public class EnclosedExtendsEnclosingTest extends TestCase {

  public void testNoContracts() {
    /*
     * Simply check if the annotation processor can handle this
     * configuration. In the past there was bug that caused infinite recursion
     * with this.
     */
    EnclosedExtendsEnclosingNoContracts o =
        new EnclosedExtendsEnclosingNoContracts.Enclosed(0);
  }

  public void testContracts() {
    try {
      EnclosedExtendsEnclosing o = new EnclosedExtendsEnclosing.Enclosed(0);
    } catch (InvariantError e) {
      /* Bogus implementation. */
    }
  }
}
