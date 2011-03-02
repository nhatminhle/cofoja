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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An abstract model element representing an element that can accept
 * type parameters.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({
  "getTypeParameters() != null",
  "!getTypeParameters().contains(null)"
})
public abstract class GenericElementModel extends QualifiedElementModel {
  /**
   * The list of type parameters of this type; empty if not a generic
   * type.
   */
  protected List<TypeName> typeParameters;

  /**
   * Constructs a new TypeModel of the specified kind, with the
   * specified name.
   */
  @Requires({
    "kind != null",
    "name != null"
  })
  protected GenericElementModel(ElementKind kind, String name) {
    super(kind, name);
    typeParameters = new ArrayList<TypeName>();
  }

  /**
   * Constructs a clone of {@code that}. The new object is identical
   * to the original <em>except</em> it has no enclosing element.
   */
  @Requires("that != null")
  protected GenericElementModel(GenericElementModel that) {
    super(that);
    typeParameters = new ArrayList<TypeName>(that.typeParameters);
  }

  @Override
  protected GenericElementModel clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  public List<? extends TypeName> getTypeParameters() {
    return Collections.unmodifiableList(typeParameters);
  }

  @Ensures("getTypeParameters().isEmpty()")
  public void clearTypeParameters() {
    typeParameters.clear();
  }

  @Requires("typeName != null")
  @Ensures({
    "getTypeParameters().size() == old(getTypeParameters().size()) + 1",
    "getTypeParameters().contains(typeName)"
  })
  public void addTypeParameter(TypeName typeName) {
    typeParameters.add(typeName);
  }
}
