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
package com.google.java.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies exceptional postconditions that apply to the annotated
 * method. Exceptional postconditions apply whenever the annotated
 * method exits by throwing an exception, in contrast to normal
 * postconditions that apply when the method exits normally. The
 * annotated method must establish its postconditions if and only if
 * the preconditions were satisfied.
 *
 * <p>When run time checking of contracts is enabled, exceptional
 * postconditions are checked at method exit, when the method exits by
 * throwing an exception, and throw a
 * {@link com.google.java.contract.PostconditionError} when they are violated.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @see Ensures
 */
@Documented
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface ThrowEnsures {
  /**
   * The alternating list of signal--postcondition pairs that must be
   * met on throw. The expressions must be valid Java code and can
   * reference all things visible to the class, as well as the
   * method's arguments. Since exceptional postconditions may need to
   * refer to the old value of an expression and the exception object
   * thrown by the annotated method, the following extensions are
   * allowed:
   *
   * <p>The {@code signal} keyword refers to the object thrown
   * by the method, and has for static type
   * {@link java.lang.Throwable}.
   *
   * <p>The {@code old()} construct has the same syntax and semantics
   * as in normal postconditions.
   *
   * @see Ensures#value()
   */
  String[] value();
}
