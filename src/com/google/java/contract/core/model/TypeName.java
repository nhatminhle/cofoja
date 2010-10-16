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
package com.google.java.contract.core.model;

import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;

/**
 * A type name, as it is used in source files.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant("declaredName != null")
public class TypeName {
  protected String declaredName;

  /**
   * Constructs a new empty TypeName. This constructor is provided for
   * convenience; {@link #declaredName} must be set on return from the
   * child constructor.
   */
  protected TypeName() {
  }

  /**
   * Constructs a new TypeName from its declared name.
   */
  @Requires("declaredName != null")
  @Ensures("declaredName.equals(getDeclaredName())")
  public TypeName(String declaredName) {
    this.declaredName = declaredName;
  }

  @Ensures("result != null")
  public String getDeclaredName() {
    return declaredName;
  }

  /**
   * Returns a string representation of this name usable in Java code.
   */
  @Override
  public String toString() {
    return declaredName;
  }
}
