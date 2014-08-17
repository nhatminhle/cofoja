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

import com.google.java.contract.ContractImport;
import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A model element representing one of the com.google.java.contract.* annotations.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@ContractImport({
  "com.google.java.contract.util.Iterables",
  "com.google.java.contract.util.Predicates"
})
@Invariant({
  "!isWeakVirtual() || isVirtual()",
  "getValues() != null",
  "!getValues().contains(null)",
  "Iterables.all(getLineNumbers(), " +
      "Predicates.or(Predicates.isNull(), Predicates.between(1L, null)))",
  "getValues().size() == getLineNumbers().size()",
  "getOwner() != null"
})
public class ContractAnnotationModel extends ElementModel {
  /**
   * {@code true} if this annotation is directly present on the
   * annotated element, that is, it is not inherited through any of
   * its interfaces or its superclass.
   */
  protected boolean primary;

  /**
   * {@code true} if this annotation denotes a virtual contract. A
   * virtual contract is one that is inherited through the normal
   * (virtual) class hierarchy, and not through an interface.
   */
  protected boolean virtual;

  /**
   * {@code true} if this annotation denotes a contract that is
   * virtual but does not require a stub to be generated.
   */
  protected boolean weakVirtual;

  /**
   * The name of the owner type of this annotation, from which it is
   * inherited.
   */
  protected ClassName owner;

  /**
   * The return type of the method contracted by this annotation; it
   * is required for stubbing since return types may change
   * covariantly with subtyping in Java.
   */
  protected TypeName returnType;

  /**
   * The values of this annotation. These are assertion expressions,
   * as strings.
   */
  protected List<String> values;

  /**
   * The line numbers associated with the values of this expression,
   * in the original source file (each line number may be
   * {@code null}).
   */
  protected List<Long> lineNumbers;

  /**
   * Constructs a new ContractAnnotationModel.
   *
   * @param kind the kind of this element
   * @param primary whether the annotation is directly present on the
   * annotated element
   * @param virtual {@code true} if the contract introduced by this
   * annotation requires a stub
   * @param owner the name of the class where this annotation is
   * inherited from
   * @param returnType the return type of the contracted method,
   * if any
   */
  @Requires({
    "kind != null",
    "owner != null",
    "kind.isSourceAnnotation()"
  })
  public ContractAnnotationModel(ElementKind kind, boolean primary,
                                 boolean virtual, ClassName owner,
                                 TypeName returnType) {
    super(kind, "<@" + kind.name() + ">");
    this.primary = primary;
    this.virtual = virtual;
    weakVirtual = false;
    this.owner = owner;
    this.returnType = returnType;
    values = new ArrayList<String>();
    lineNumbers = new ArrayList<Long>();
  }

  /**
   * Constructs a clone of {@code that}. The new object is identical
   * to the original <em>except</em> it has no enclosing element.
   */
  @Requires("that != null")
  @Ensures("getEnclosingElement() == null")
  public ContractAnnotationModel(ContractAnnotationModel that) {
    super(that);
    primary = that.primary;
    virtual = that.virtual;
    weakVirtual = that.weakVirtual;
    owner = that.owner;
    returnType = that.returnType;
    values = new ArrayList<String>(that.values);
    lineNumbers = new ArrayList<Long>(that.lineNumbers);
  }

  @Override
  public ContractAnnotationModel clone() {
    return new ContractAnnotationModel(this);
  }

  public boolean isPrimary() {
    return primary;
  }

  public boolean isVirtual() {
    return virtual;
  }

  public boolean isWeakVirtual() {
    return weakVirtual;
  }

  public void setWeakVirtual(boolean weakVirtual) {
    this.weakVirtual = weakVirtual;
  }

  public ClassName getOwner() {
    return owner;
  }

  public TypeName getReturnType() {
    return returnType;
  }

  public List<String> getValues() {
    return Collections.unmodifiableList(values);
  }

  public List<Long> getLineNumbers() {
    return Collections.unmodifiableList(lineNumbers);
  }

  @Ensures({
    "getValues().isEmpty()",
    "getLineNumbers().isEmpty()"
  })
  public void clearValues() {
    values.clear();
    lineNumbers.clear();
  }

  @Requires("value != null")
  @Ensures({
    "getValues().size() == old(getValues().size()) + 1",
    "getValues().contains(value.replace('\\r', ' ').replace('\\n', ' '))",
    "getLineNumbers().size() == old(getLineNumbers().size()) + 1",
    "getLineNumbers().contains(lineNumber)"
  })
  public void addValue(String value, Long lineNumber) {
    values.add(value.replace('\r', ' ').replace('\n', ' '));
    lineNumbers.add(lineNumber);
  }

  /**
   * Returns {@code true} if the specified argument is equal to this
   * object. Two ContractAnnotationModel objects are equal if they
   * are of the same kind and have the same values.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ContractAnnotationModel)) {
      return false;
    }

    ContractAnnotationModel annotation = (ContractAnnotationModel) obj;
    return annotation.getKind() == getKind()
        && annotation.getValues().equals(getValues());
  }

  @Override
  public void accept(ElementVisitor visitor) {
    visitor.visitContractAnnotation(this);
  }
}
