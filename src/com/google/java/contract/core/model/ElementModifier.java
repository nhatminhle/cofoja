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
package com.google.java.contract.core.model;

import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;

import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.element.Modifier;

/**
 * A Java declaration modifier. In general, modifiers can be applied
 * to types, fields, and methods.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public enum ElementModifier {
  /**
   * The public modifier.
   */
  PUBLIC,

  /**
   * The protected modifier.
   */
  PROTECTED,

  /**
   * The package private modifier. This is the default visibility.
   */
  PACKAGE_PRIVATE,

  /**
   * The private modifier.
   */
  PRIVATE,

  /**
   * The modifier static.
   */
  STATIC,

  /**
   * The final modifier.
   */
  FINAL,

  /**
   * The volatile modifier.
   */
  VOLATILE,

  /**
   * The transient modifier .
   */
  TRANSIENT,

  /**
   * The abstract modifier.
   */
  ABSTRACT,

  /**
   * The synchronized modifier.
   */
  SYNCHRONIZED,

  /**
   * The native modifier.
   */
  NATIVE;

  public boolean isVisibility() {
    switch (this) {
      case PUBLIC:
      case PROTECTED:
      case PACKAGE_PRIVATE:
      case PRIVATE:
        return true;
      default:
        return false;
    }
  }

  @Requires("modifiers != null")
  public static ElementModifier visibilityIn(
      EnumSet<ElementModifier> modifiers) {
    for (ElementModifier modifier : modifiers) {
      if (modifier.isVisibility()) {
        return modifier;
      }
    }
    return null;
  }

  /**
   * Converts a set of {@link Modifier} objects into a set of
   * {@link ElementModifier} objects.
   */
  @Requires("modifiers != null")
  @Ensures("result != null")
  public static EnumSet<ElementModifier> forModifiers(
      Set<? extends Modifier> modifiers){
    EnumSet<ElementModifier> result = EnumSet.noneOf(ElementModifier.class);

    if (!modifiers.contains(Modifier.PUBLIC)
       && !modifiers.contains(Modifier.PROTECTED)
       && !modifiers.contains(Modifier.PRIVATE)) {
      result.add(ElementModifier.PACKAGE_PRIVATE);
    }

    for (Modifier modifier : modifiers) {
      switch (modifier) {
        case PUBLIC:
          result.add(ElementModifier.PUBLIC);
          break;
        case PROTECTED:
          result.add(ElementModifier.PROTECTED);
          break;
        case PRIVATE:
          result.add(ElementModifier.PRIVATE);
          break;
        case STATIC:
          result.add(ElementModifier.STATIC);
          break;
        case FINAL:
          result.add(ElementModifier.FINAL);
          break;
        case VOLATILE:
          result.add(ElementModifier.VOLATILE);
          break;
        case TRANSIENT:
          result.add(ElementModifier.TRANSIENT);
          break;
        case ABSTRACT:
          result.add(ElementModifier.ABSTRACT);
          break;
        case SYNCHRONIZED:
          result.add(ElementModifier.SYNCHRONIZED);
          break;
        case NATIVE:
          result.add(ElementModifier.NATIVE);
          break;
        default:
          /* Unsupported modifier. */
          break;
      }
    }
    return result;
  }

  /**
   * Returns the Java source code representation of this modifier, or
   * the empty string if none.
   */
  @Override
  public String toString() {
    switch (this) {
      case PUBLIC:
      case PROTECTED:
      case PRIVATE:
      case STATIC:
      case FINAL:
      case VOLATILE:
      case ABSTRACT:
      case SYNCHRONIZED:
        return name().toLowerCase();
      default:
        return "";
    }
  }
}
