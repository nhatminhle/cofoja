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
 * Specifies preconditions that apply to the annotated method. Callers
 * must establish the preconditions of methods they call.
 *
 * <p>When run time checking of contracts is enabled, preconditions
 * are checked at method entry and throw a
 * {@link com.google.java.contract.PreconditionError} when they are violated.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Documented
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface Requires {
  /**
   * The list of precondition expressions that must be met by the
   * annotated method, written as strings. The expressions must be
   * valid Java code and can reference all things visible to every
   * caller of the method, as well as the method's arguments.
   *
   * <p>Expressions may also reference things that are not visible
   * to the caller, such as private fields when the method is public,
   * but this is considered bad style.
   */
  String[] value();
}
