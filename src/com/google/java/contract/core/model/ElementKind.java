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

import com.google.java.contract.Requires;

/**
 * The kind of an {@link Element}.
 */
public enum ElementKind {
  /*
   * Source elements.
   */

  /**
   * A source file.
   */
  SOURCE,

  /**
   * A class.
   */
  CLASS,

  /**
   * An interface.
   */
  INTERFACE,

  /**
   * An annotation.
   */
  ANNOTATION_TYPE,

  /**
   * An enum.
   */
  ENUM,

  /**
   * An enum constant.
   */
  CONSTANT,

  /**
   * A field. Mocked in output.
   */
  FIELD,

  /**
   * A method that is not a constructor. Mocked in output.
   */
  METHOD,

  /**
   * A constructor. Mocked in output.
   */
  CONSTRUCTOR,

  /**
   * A method parameter.
   */
  PARAMETER,

  /**
   * An @Invariant annotation.
   */
  INVARIANT,

  /**
   * An @Requires annotation. Source-only, not present in output.
   */
  REQUIRES,

  /**
   * An @Ensures annotation. Source-only, not present in output.
   */
  ENSURES,

  /**
   * An @ThrowEnsures annotation. Source-only, not present in output.
   */
  THROW_ENSURES,

  /*
   * Output elements.
   */

  /**
   * An @ContractMethodSignature annotation.
   */
  CONTRACT_SIGNATURE,

  /**
   * A contract method.
   */
  CONTRACT_METHOD,

  /**
   * A mock contract method.
   */
  CONTRACT_MOCK;

  public boolean isType() {
    switch (this) {
      case CLASS:
      case ENUM:
      case INTERFACE:
      case ANNOTATION_TYPE:
        return true;
      default:
        return false;
    }
  }

  public boolean isMember() {
    switch (this) {
      case FIELD:
      case METHOD:
      case CONSTRUCTOR:
      case CONTRACT_METHOD:
      case CONTRACT_MOCK:
        return true;
      default:
        return false;
    }
  }

  @Requires("isMember()")
  public boolean isMock() {
    switch (this) {
      case METHOD:
      case CONSTRUCTOR:
      case CONTRACT_MOCK:
        return true;
      default:
        return false;
    }
  }

  @Requires("isMember()")
  public boolean isContract() {
    return !isMock();
  }

  public boolean isAnnotation() {
    switch (this) {
      case INVARIANT:
      case REQUIRES:
      case ENSURES:
      case THROW_ENSURES:
      case CONTRACT_SIGNATURE:
        return true;
      default:
        return false;
    }
  }

  public boolean isInterfaceType() {
    switch(this) {
      case INTERFACE:
      case ANNOTATION_TYPE:
        return true;
      default:
        return false;
    }
  }

  public boolean isSourceAnnotation() {
    switch (this) {
      case INVARIANT:
      case REQUIRES:
      case ENSURES:
      case THROW_ENSURES:
        return true;
      default:
        return false;
    }
  }
}
