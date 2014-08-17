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
import com.google.java.contract.util.Predicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * An abstract model element.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@ContractImport("com.google.java.contract.util.Iterables")
@Invariant({
  "getEnclosingElement() == null " +
      "|| getEnclosingElement().getEnclosedElements().contains(this)",
  "Iterables.all(getEnclosedElements(), isValidEnclosedElement())",
  "getKind() != null",
  "getSimpleName() != null"
})
public abstract class ElementModel implements Cloneable {
  /**
   * The kind of this element.
   */
  protected ElementKind kind;

  /**
   * The simple name of this element. The simple name is the relative
   * identifier used to refer to this element in source code.
   *
   * <p>This name must be kept synchronized with other forms, if any.
   */
  protected String simpleName;

  /**
   * The parent element of this element.
   */
  protected ElementModel enclosingElement;

  /**
   * The child elements of this element. Each kind refines its own
   * enclosing--enclosed relationship with other kinds.
   *
   * <p>This list must contain all enclosed elements but need not be
   * the only such container. Specialized, partial subsets of enclosed
   * elements, if any, must be kept synchronized with this field.
   */
  protected List<ElementModel> enclosedElements;

  /**
   * An object intended to hold information on the source of this
   * model element.
   */
  protected Object sourceInfo;

  /**
   * Constructs a new ElementModel of the specified kind, with the
   * specified simple name.
   */
  @Requires({
    "kind != null",
    "name != null"
  })
  protected ElementModel(ElementKind kind, String name) {
    this.kind = kind;
    simpleName = name;
    enclosingElement = null;
    enclosedElements = new ArrayList<ElementModel>();
    sourceInfo = null;
  }

  /**
   * Constructs a clone of {@code that}. The new object is identical
   * to the original <em>except</em> it has no enclosing element.
   */
  @Requires("that != null")
  @Ensures("getEnclosingElement() == null")
  protected ElementModel(ElementModel that) {
    kind = that.kind;
    simpleName = that.simpleName;
    enclosingElement = null;
    enclosedElements =
        new ArrayList<ElementModel>(that.enclosedElements.size());
    ArrayList<ElementModel> elements =
        new ArrayList<ElementModel>(that.enclosedElements);
    for (ElementModel element : elements) {
      try {
        addEnclosedElement(element.clone());
      } catch (CloneNotSupportedException e) {
        /*
         * Not reached. Only abstract classes in the hierarchy will
         * throw CloneNotSupportedException.
         */
      }
    }
    sourceInfo = that.sourceInfo;
  }

  @Override
  @Ensures({
    "result != null",
    "result.getEnclosingElement() == null"
  })
  protected ElementModel clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  public ElementKind getKind() {
    return kind;
  }

  public List<? extends ElementModel> getEnclosedElements() {
    return Collections.unmodifiableList(enclosedElements);
  }

  /**
   * Adds a child element at the specified position in the enclosed
   * element list. First, the added element is detached from its
   * previous parent, if any.
   */
  @Requires({
    "index >= 0",
    "index <= element.getEnclosedElements().size()",
    "isValidEnclosedElement(element)"
  })
  @Ensures("enclosedElements.contains(element)")
  protected void addEnclosedElement(int index, ElementModel element) {
    fixEnclosedElement(element);
    enclosedElements.add(index, element);
  }

  /**
   * Adds a child element to the enclosed element list. First, the
   * added element is detached from its previous parent, if any.
   */
  @Requires("isValidEnclosedElement(element)")
  @Ensures("getEnclosedElements().contains(element)")
  public void addEnclosedElement(ElementModel element) {
    fixEnclosedElement(element);
    enclosedElements.add(element);
  }

  private void fixEnclosedElement(ElementModel element) {
    if (element.enclosingElement != null) {
      element.enclosingElement.removeEnclosedElement(element);
    }
    element.enclosingElement = this;
  }

  @Requires("isValidEnclosedElement(element)")
  @Ensures("!getEnclosedElements().contains(element)")
  public void removeEnclosedElement(ElementModel element) {
    enclosedElements.remove(element);
    element.enclosingElement = null;
  }

  public ElementModel getEnclosingElement() {
    return enclosingElement;
  }

  @Requires("element == null || isValidEnclosedElement(element)")
  @Ensures({
    "element == getEnclosingElement()",
    "element.getEnclosedElements().contains(this)"
  })
  public void setEnclosingElement(ElementModel element) {
    element.addEnclosedElement(this);
  }

  public String getSimpleName() {
    return simpleName;
  }

  public Object getSourceInfo() {
    return sourceInfo;
  }

  public void setSourceInfo(Object info) {
    sourceInfo = info;
  }

  /**
   * Visits this element with the specified visitor.
   */
  @Requires("visitor != null")
  public abstract void accept(ElementVisitor visitor);

  /**
   * Returns {@code true} if the specified argument is equal to this
   * object. Two ElementModel are equal if they have the same simple
   * name and their enclosing elements are equal.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ElementModel)) {
      return false;
    }

    ElementModel element = (ElementModel) obj;
    return element.getSimpleName().equals(getSimpleName())
        && element.getEnclosingElement().equals(getEnclosingElement());
  }

  @Override
  public int hashCode() {
    return 31 * simpleName.hashCode() + getEnclosingElement().hashCode();
  }

  @Override
  public String toString() {
    return simpleName;
  }

  /**
   * Returns {@code true} if {@code element} can be added (as an
   * enclosed element) to this element.
   */
  public boolean isValidEnclosedElement(ElementModel element) {
    return element != null
        && getAllowedEnclosedKinds().contains(element.getKind());
  }

  private final Predicate<ElementModel> isValidEnclosedElementPredicate =
      new Predicate<ElementModel>() {
    @Override
    public boolean apply(ElementModel element) {
      return isValidEnclosedElement(element);
    }
  };

  public Predicate<ElementModel> isValidEnclosedElement() {
    return isValidEnclosedElementPredicate;
  }

  /**
   * Reflectively returns the kinds of elements that are allowed as
   * children of this element.
   */
  @Ensures("result != null")
  public EnumSet<ElementKind> getAllowedEnclosedKinds() {
    return EnumSet.noneOf(ElementKind.class);
  }
}
