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

import java.util.EnumSet;

/**
 * An abstract model element that can be qualified with modifiers.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant("getModifiers() != null")
public abstract class QualifiedElementModel extends ElementModel {
  /**
   * The modifiers that apply to this element.
   */
  protected EnumSet<ElementModifier> modifiers;

  /**
   * Constructs a new QualifiedElementModel of the specified kind,
   * with the specified simple name.
   */
  @Requires({
    "kind != null",
    "name != null"
  })
  protected QualifiedElementModel(ElementKind kind, String name) {
    super(kind, name);
    modifiers = EnumSet.noneOf(ElementModifier.class);
  }

  @Requires("that != null")
  @Ensures("getEnclosingElement() == null")
  protected QualifiedElementModel(QualifiedElementModel that) {
    super(that);
    modifiers = EnumSet.copyOf(that.modifiers);
  }

  /**
   * Constructs a clone of {@code that}. The new object is identical
   * to the original <em>except</em> it has no enclosing element.
   */
  @Override
  protected QualifiedElementModel clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  public EnumSet<ElementModifier> getModifiers() {
    return modifiers;
  }

  /**
   * Adds a modifier to this element. This method ensures that exactly
   * one visibility modifier is set.
   */
  @Requires("modifier != null")
  @Ensures("getModifiers().contains(modifier)")
  public void addModifier(ElementModifier modifier) {
    switch (modifier) {
      case PRIVATE:
      case PACKAGE_PRIVATE:
      case PROTECTED:
      case PUBLIC:
        modifiers.remove(ElementModifier.PRIVATE);
        modifiers.remove(ElementModifier.PACKAGE_PRIVATE);
        modifiers.remove(ElementModifier.PROTECTED);
        modifiers.remove(ElementModifier.PUBLIC);
    }
    modifiers.add(modifier);
  }

  /**
   * Removes a modifier from this element. This method ensures that
   * exactly one visibility modifier is set. It defaults to
   * package-private if no other visibility is set.
   */
  @Requires("modifier != null")
  @Ensures("!getModifiers().contains(modifier)")
  public void removeModifier(ElementModifier modifier) {
    modifiers.remove(modifier);
    switch (modifier) {
      case PRIVATE:
      case PACKAGE_PRIVATE:
      case PROTECTED:
      case PUBLIC:
        modifiers.add(ElementModifier.PACKAGE_PRIVATE);
    }
  }
}
