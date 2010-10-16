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

import com.google.java.contract.SpecificationError;
import junit.framework.TestCase;

/**
 * Tests that {@link ContractedChecker#assertLoadedClassesContracted}
 * detects a class which is missing the {@link Contracted} annotation.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class ContractedCheckTest extends TestCase {
  private static class ShouldBeContractedClass extends ContractedClass {
  }

  public void testContractedCheck() {
    ShouldBeContractedClass sample = new ShouldBeContractedClass();
    try {
      Cofoja.contractEnv.assertLoadedClassesContracted();
      fail();
    } catch (SpecificationError expected) {
      /* Expected exception. */
    }
  }
}
