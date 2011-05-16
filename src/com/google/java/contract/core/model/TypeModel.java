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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A model element representing a type.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({
  "getName() != null",
  "getSimpleName().equals(getName().getSimpleName())",
  "getInterfaces() != null",
  "!getInterfaces().contains(null)",
  "getSuperArguments() != null",
  "!getSuperArguments().contains(null)",
  "getSuperclass() != null || superArguments.isEmpty()",
  "getImportNames() != null",
  "!getImportNames().contains(null)",
  "getEnclosingElement() == null || importNames.isEmpty()"
})
public class TypeModel extends GenericElementModel {
  /**
   * The name of this type.
   */
  protected ClassName name;

  /**
   * The names of interfaces implemented by this type.
   */
  protected Set<ClassName> interfaces;

  /**
   * The name of the superclass of this type, if any.
   */
  protected ClassName superclass;

  /**
   * The types of the parameters to pass to the superclass constructor
   * call.
   */
  protected List<TypeName> superArguments;

  /**
   * The values of import statements in effect in this source file.
   */
  protected Set<String> importNames;

  /**
   * Constructs a new TypeModel of the specified kind, with the
   * specified name.
   */
  @Requires({
    "kind != null",
    "name != null",
    "kind.isType()"
  })
  public TypeModel(ElementKind kind, ClassName name) {
    super(kind, name.getSimpleName());
    this.name = name;
    interfaces = new HashSet<ClassName>();
    superArguments = new ArrayList<TypeName>();
    importNames = new HashSet<String>();
  }

  /**
   * Constructs a clone of {@code that}. The new object is identical
   * to the original <em>except</em> it has no enclosing element.
   */
  @Requires("that != null")
  @Ensures("getEnclosingElement() == null")
  public TypeModel(TypeModel that) {
    super(that);
    name = that.name;
    interfaces = new HashSet<ClassName>(that.interfaces);
    superArguments = new ArrayList<TypeName>(that.superArguments);
    importNames = new HashSet<String>(that.importNames);
  }

  @Override
  public TypeModel clone() {
    return new TypeModel(this);
  }

  public ClassName getName() {
    return name;
  }

  @Requires({
    "getEnclosingElement() == null",
    "value != null"
  })
  @Ensures("getImportNames().contains(value)")
  public void addImportName(String value) {
    importNames.add(value);
  }

  @Requires({
    "getEnclosingElement() == null",
    "value != null"
  })
  @Ensures("!getImportNames().contains(value)")
  public void removeImportName(String value) {
    importNames.remove(value);
  }

  @Requires("getEnclosingElement() == null")
  public Set<String> getImportNames() {
    return Collections.unmodifiableSet(importNames);
  }

  public ClassName getSuperclass() {
    return superclass;
  }

  public void setSuperclass(ClassName className) {
    superclass = className;
  }

  @Ensures("result != null")
  public List<? extends TypeName> getSuperArguments() {
    return Collections.unmodifiableList(superArguments);
  }

  @Ensures("getSuperArguments().isEmpty()")
  public void clearSuperArguments() {
    superArguments.clear();
  }

  @Requires("typeName != null")
  @Ensures({
    "getSuperArguments().size() == old(getSuperArguments().size()) + 1",
    "getSuperArguments().contains(typeName)"
  })
  public void addSuperArgument(TypeName typeName) {
    superArguments.add(typeName);
  }

  public Set<? extends ClassName> getInterfaces() {
    return Collections.unmodifiableSet(interfaces);
  }

  @Requires("className != null")
  @Ensures("getInterfaces().contains(className)")
  public void addInterface(ClassName className) {
    interfaces.add(className);
  }

  @Requires("className != null")
  @Ensures("!getInterfaces().contains(className)")
  public void removeInterface(ClassName className) {
    interfaces.remove(className);
  }

  @Ensures("result != null")
  public List<? extends ContractAnnotationModel> getInvariants() {
    return Elements.filter(enclosedElements, ContractAnnotationModel.class,
                           ElementKind.INVARIANT);
  }

  @Requires({
    "annotation != null",
    "annotation.getKind() == ElementKind.INVARIANT"
  })
  @Ensures({
    "getEnclosedElements().contains(annotation)",
    "getInvariants().contains(annotation)"
  })
  public void addInvariant(ContractAnnotationModel annotation) {
    addEnclosedElement(annotation);
  }

  @Requires({
    "annotation != null",
    "annotation.getKind() == ElementKind.INVARIANT"
  })
  @Ensures({
    "!getEnclosedElements().contains(annotation)",
    "!getInvariants().contains(annotation)"
  })
  public void removeInvariant(ContractAnnotationModel annotation) {
    removeEnclosedElement(annotation);
  }

  @Ensures({
    "result != null",
    "!result.contains(null)"
  })
  public List<? extends QualifiedElementModel> getMembers() {
    return Elements.filter(enclosedElements, QualifiedElementModel.class,
                           ElementKind.CLASS,
                           ElementKind.ENUM,
                           ElementKind.INTERFACE,
                           ElementKind.ANNOTATION_TYPE,
                           ElementKind.FIELD,
                           ElementKind.METHOD,
                           ElementKind.CONSTRUCTOR,
                           ElementKind.CONTRACT_METHOD,
                           ElementKind.CONTRACT_MOCK);
  }

  @Requires({
    "member != null",
    "member.getKind().isMember()"
  })
  @Ensures({
    "getEnclosedElements().contains(member)",
    "getMembers().contains(member)"
  })
  public void addMember(QualifiedElementModel member) {
    addEnclosedElement(member);
  }

  @Requires({
    "member != null",
    "member.getKind().isMember()"
  })
  @Ensures({
    "!getEnclosedElements().contains(member)",
    "!getMembers().contains(member)"
  })
  public void removeInvariant(QualifiedElementModel member) {
    removeEnclosedElement(member);
  }

  @Override
  public void accept(ElementVisitor visitor) {
    visitor.visitType(this);
  }

  @Override
  public EnumSet<ElementKind> getAllowedEnclosedKinds() {
    EnumSet<ElementKind> allowed =
        EnumSet.of(ElementKind.CLASS,
                   ElementKind.ENUM,
                   ElementKind.INTERFACE,
                   ElementKind.ANNOTATION_TYPE,
                   ElementKind.CONSTANT,
                   ElementKind.FIELD,
                   ElementKind.METHOD,
                   ElementKind.CONSTRUCTOR,
                   ElementKind.INVARIANT,
                   ElementKind.CONTRACT_METHOD,
                   ElementKind.CONTRACT_MOCK);
    allowed.addAll(super.getAllowedEnclosedKinds());
    return allowed;
  }
}
