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
import com.google.java.contract.PreconditionError;
import com.google.java.contract.Requires;

import junit.framework.TestCase;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

/**
 * Tests that the annotation processor handles correctly contracts applied
 * to annotation types, ignoring them, but maintaining annotation's
 * informations.
 *
 * @author chatain@google.com (Leonardo Chatain)
 */
public class ContractedAnnotationTest extends TestCase {
  /**
   * If this annotation verifies it's contracts, a failure should be produced.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Invariant("false")
  private static @interface MyAnnotation {
    @Requires("false")
    String value() default "none";
  }

  /**
   * Tester class, uses the annotation defined above.
   */
  @MyAnnotation("X")
  private static class UsesAnnotations {
    @MyAnnotation
    public void m1() {}
    @MyAnnotation
    public void m2() {}
    @MyAnnotation
    public void m3() {}
    @MyAnnotation
    public void m4() {}

    @Requires("anno.value().equals(\"error\")")
    public void trickyContract(MyAnnotation anno) {}

    @Requires("anno.value().equals(\"none\")")
    public void trickyContractDefaultValue(MyAnnotation anno) {}
  }

  UsesAnnotations uses;

  public void testInnerAnnotation() {
    uses = new UsesAnnotations();
  }

  /*
   * Test that annotations effectively do not trigger contracts.
   */
  public void testEmptyAnnotationContracts() {
    Class<UsesAnnotations> ua = UsesAnnotations.class;
    MyAnnotation anno = ua.getAnnotation(MyAnnotation.class);
    assertEquals("X", anno.value());

    /* All methods are annotated with the default value. */
    for (Method m : ua.getMethods()) {
      anno = m.getAnnotation(MyAnnotation.class);
      if (anno != null) {
        assertEquals("none", anno.value());
      }
    }

  }

  public void testAnnotationOnContract() throws SecurityException,
                                                NoSuchMethodException {
    uses = new UsesAnnotations();
    Class<UsesAnnotations> ua = UsesAnnotations.class;
    MyAnnotation xanno = ua.getAnnotation(MyAnnotation.class);
    try {
      uses.trickyContract(xanno);
      fail();
    } catch (PreconditionError e) {
      /* Should catch this. */
    }

    Method m1 = ua.getMethod("m1", (Class<?>[]) null);
    MyAnnotation danno = m1.getAnnotation(MyAnnotation.class);
    try {
      uses.trickyContractDefaultValue(danno);
    } catch (PreconditionError e) {
      fail();
    }
  }
}
