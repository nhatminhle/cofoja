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
import com.google.java.contract.core.util.Elements;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A model element representing a method.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({
  "getExceptions() != null",
  "isConstructor() == (returnType == null)"
})
public class MethodModel extends GenericElementModel {
  /**
   * The list of exceptions that can be thrown by this method.
   */
  protected Set<TypeName> exceptions;

  /**
   * The return type of this method, or {@code null} if this is a
   * constructor.
   */
  protected TypeName returnType;

  /**
   * Whether this method accepts variable-length argument lists.
   */
  protected boolean variadic;

  /**
   * Constructs a new MethodModel of the specified kind, which is not
   * a constructor, with the specified name and return type.
   */
  @Requires({
    "kind != null",
    "kind.isMember() && kind != ElementKind.FIELD",
    "kind != ElementKind.CONSTRUCTOR",
    "name != null",
    "returnType != null"
  })
  public MethodModel(ElementKind kind, String name, TypeName returnType) {
    super(kind, name);
    exceptions = new HashSet<TypeName>();
    this.returnType = returnType;
    variadic = false;
  }

  /**
   * Constructs a clone of {@code that}. The new object is identical
   * to the original <em>except</em> it has no enclosing element.
   */
  @Requires("that != null")
  @Ensures("getEnclosingElement() == null")
  public MethodModel(MethodModel that) {
    super(that);
    exceptions = new HashSet<TypeName>(that.exceptions);
    returnType = that.returnType;
    variadic = false;
  }

  @Override
  public MethodModel clone() {
    return new MethodModel(this);
  }

  /**
   * Constructs a new constructor model.
   */
  public MethodModel() {
    super(ElementKind.CONSTRUCTOR, "<init>");
    exceptions = new HashSet<TypeName>();
    returnType = null;
    variadic = false;
  }

  public Set<? extends TypeName> getExceptions() {
    return Collections.unmodifiableSet(exceptions);
  }

  @Requires("exception != null")
  @Ensures("getExceptions().contains(exception)")
  public void addException(TypeName exception) {
    exceptions.add(exception);
  }

  @Requires("exception != null")
  @Ensures("!getExceptions().contains(exception)")
  public void removeException(TypeName exception) {
    exceptions.remove(exception);
  }

  @Requires("!isConstructor()")
  @Ensures("result != null")
  public TypeName getReturnType() {
    return returnType;
  }

  @Requires({
    "!isConstructor()",
    "type != null"
  })
  public void setReturnType(TypeName type) {
    returnType = type;
  }

  public boolean isVariadic() {
    return variadic;
  }

  public void setVariadic(boolean variadic) {
    this.variadic = variadic;
  }

  public boolean isConstructor() {
    return kind == ElementKind.CONSTRUCTOR;
  }

  @Ensures("result != null")
  public List<? extends VariableModel> getParameters() {
    return Elements.filter(getEnclosedElements(), VariableModel.class,
                           ElementKind.PARAMETER);
  }

  @Requires({
    "param != null",
    "param.getKind() == ElementKind.PARAMETER"
  })
  @Ensures({
    "getEnclosedElements().contains(param)",
    "getParameters().contains(param)"
  })
  public void addParameter(VariableModel param) {
    addEnclosedElement(param);
  }

  @Requires({
    "param != null",
    "param.getKind() == ElementKind.PARAMETER"
  })
  @Ensures({
    "!getEnclosedElements().contains(param)",
    "!getParameters().contains(param)"
  })
  public void removeParameter(VariableModel param) {
    removeEnclosedElement(param);
  }

  @Override
  public void accept(ElementVisitor visitor) {
    visitor.visitMethod(this);
  }

  @Override
  public EnumSet<ElementKind> getAllowedEnclosedKinds() {
    EnumSet<ElementKind> allowed =
        EnumSet.of(ElementKind.PARAMETER,
                   ElementKind.REQUIRES,
                   ElementKind.ENSURES,
                   ElementKind.THROW_ENSURES);
    allowed.addAll(super.getAllowedEnclosedKinds());
    return allowed;
  }

  /**
   * Returns {@code true} if the specified argument is equal to this
   * object. Two MethodModel objects are equal if they have the same
   * simple name and parameter types and their enclosing elements are
   * equal. Equality for MethodModel objects is loose: two methods can
   * be <em>unequal</em> and still conflict with one another; this
   * implementation only guarantees that objects representing
   * different methods will not be compared equal.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof MethodModel)) {
      return false;
    }

    MethodModel method = (MethodModel) obj;
    if (!method.getSimpleName().equals(getSimpleName())) {
      return false;
    }
    List<? extends VariableModel> params = getParameters();
    List<? extends VariableModel> others = method.getParameters();
    if (params.size() != others.size()) {
      return false;
    }
    Iterator<? extends VariableModel> itParams = params.iterator();
    Iterator<? extends VariableModel> itOthers = others.iterator();
    while (itParams.hasNext()) {
      if (!itParams.next().getType().equals(itOthers.next().getType())) {
        return false;
      }
    }
    return method.getEnclosingElement().equals(getEnclosingElement());
  }
}
