/*
 * Copyright 2010 Google Inc.
 * Copyright 2011 Nhat Minh Lê
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
package com.google.java.contract.core.apt;

import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ElementModifier;
import com.google.java.contract.core.model.TypeModel;

import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementScanner6;

/**
 * An element visitor that extracts constructor arguments from a
 * callable constructor. It always picks a constructor with the
 * broadest access level: if the original class was correct, then at
 * least that one should be accessible.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant("subtype != null")
class SuperCallBuilder extends ElementScanner6<Void, Void> {
  protected FactoryUtils utils;
  protected DeclaredType typeMirror;
  protected TypeModel subtype;
  protected ElementModifier constructorFound;

  /**
   * Constructs a new SuperCallBuilder that builds a constructor call
   * for {@code subtype}. The super call will be made against
   * {@code typeMirror}, which may differ from the raw element this
   * instance is visiting (e.g. it can be a specialized version of
   * a generic class).
   */
  @Requires({
    "subtype != null",
    "utils != null"
  })
  SuperCallBuilder(DeclaredType typeMirror, TypeModel subtype,
                   FactoryUtils utils) {
    this.utils = utils;
    this.typeMirror = typeMirror;
    this.subtype = subtype;
    constructorFound = null;
  }

  @Override
  public Void visitType(TypeElement e, Void unused) {
    scan(ElementFilter.constructorsIn(e.getEnclosedElements()), null);
    return null;
  }

  @Override
  public Void visitExecutable(ExecutableElement e, Void unused) {
    ElementModifier v = ElementModifier.visibilityIn(
        ElementModifier.forModifiers(e.getModifiers()));
    if (constructorFound != null
        && constructorFound.ordinal() <= v.ordinal()) {
      return null;
    }

    subtype.clearSuperArguments();
    ExecutableType execType =
        (ExecutableType) utils.typeUtils.asMemberOf(typeMirror, e);
    List<? extends TypeMirror> paramTypes = execType.getParameterTypes();
    for (TypeMirror t : paramTypes) {
      subtype.addSuperArgument(utils.getTypeNameForType(t));
    }

    constructorFound = v;
    return null;
  }
}
