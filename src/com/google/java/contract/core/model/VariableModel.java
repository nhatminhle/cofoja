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
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;

/**
 * A model element representing a variable (this classification
 * includes parameters).
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant("type != null")
public class VariableModel extends QualifiedElementModel {
  /**
   * The type of this variable.
   */
  protected TypeName type;

  /**
   * Constructs a new VariableModel of the specified kind and type,
   * with the specified name.
   */
  @Requires({
    "kind != null",
    "name != null",
    "type != null",
    "kind == ElementKind.CONSTANT " +
        "|| kind == ElementKind.FIELD || kind == ElementKind.PARAMETER"
  })
  public VariableModel(ElementKind kind, String name, TypeName type) {
    super(kind, name);
    this.type = type;
  }

  /**
   * Constructs a clone of {@code that}. The new object is identical
   * to the original <em>except</em> it has no enclosing element.
   */
  @Requires("that != null")
  @Ensures("getEnclosingElement() == null")
  public VariableModel(VariableModel that) {
    super(that);
    type = that.type;
  }

  @Override
  public VariableModel clone() {
    return new VariableModel(this);
  }

  @Ensures("result != null")
  public TypeName getType() {
    return type;
  }

  @Requires("type != null")
  public void setType(TypeName type) {
    this.type = type;
  }

  @Override
  public void accept(ElementVisitor visitor) {
    visitor.visitVariable(this);
  }
}
