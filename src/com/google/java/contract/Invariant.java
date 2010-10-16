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
package com.google.java.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies class invariants that apply to the annotated type. The
 * annotated class must guarantee its invariants.
 *
 * <p>When run time checking of contracts is enabled, class invariants
 * are checked on entry and exit of public and package-private
 * methods, and throw a {@link com.google.java.contract.InvariantError} when
 * they are violated.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Invariant {
  /**
   * The list of invariant expressions that must be met by the
   * annotated type, written as strings. The expressions must be valid
   * Java code and can reference all things visible to the class,
   * including private members.
   */
  String[] value();
}
