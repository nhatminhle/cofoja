/*
 * Copyright 2011 Google Inc.
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

import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.runtime.BlacklistManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner6;

/**
 * Recursively scans the type tree looking for contract annotations. Results
 * are cached so there's no need to scan the same element twice.
 *
 * @author chatain@google.com (Leonardo Chatain)
 */
@Invariant("this.contractedElements != null")
public class ContractFinder
    extends ElementScanner6<Boolean, Void> {
  private FactoryUtils utils;
  private Map<TypeElement, Boolean> contractedElements;
  private BlacklistManager blackList;

  @Requires("utils != null")
  public ContractFinder(FactoryUtils utils) {
    this.utils = utils;
    this.contractedElements = new HashMap<TypeElement, Boolean>();
    this.blackList = BlacklistManager.getInstance();
  }

  @Override
  @Ensures("result != null")
  public Boolean visitType(TypeElement e, Void v) {
    /* The current element has already been analyzed. */
    if (contractedElements.containsKey(e)) {
      return contractedElements.get(e);
    }

    /* The element is in the blacklist, do not scan. */
    if (blackList.isIgnored(e.getQualifiedName().toString())) {
      contractedElements.put(e, Boolean.FALSE);
      return Boolean.FALSE;
    }

    /*
     * Before searching for contracts, add the element to the visited cache.
     * This is important to avoid recursion (e.g. when an enclosed element
     * extends the enclosing one).
     */
    contractedElements.put(e, Boolean.FALSE);
    Boolean contracted = isContractedType(e);
    contractedElements.put(e, contracted);
    return contracted;
  }

  @Override
  @Ensures("result != null")
  public Boolean visitExecutable(ExecutableElement e, Void p) {
    return hasContracts(e.getAnnotationMirrors());
  }

  @Override
  @Ensures("result != null")
  public Boolean visitVariable(VariableElement e, Void p) {
    /* Variables do not have contracts. */
    return Boolean.FALSE;
  }

  @Override
  @Ensures("result != null")
  public Boolean visitPackage(PackageElement e, Void p) {
    /* Packages do not have contracts. */
    return Boolean.FALSE;
  }

  @Override
  @Ensures("result != null")
  public Boolean visitTypeParameter(TypeParameterElement e, Void p) {
    /* Type parameters do not have contracts. */
    return Boolean.FALSE;
  }

  /**
   * Scans a {@code TypeElement} looking for contracts.
   * If contracts are not found on the type itself, scans its enclosed elements,
   * implemented interfaces and superclass.
   *
   * @param e the element to be scanned
   * @return whether the element contains contracts
   */
  @Requires("e != null")
  @Ensures("result != null")
  private Boolean isContractedType(TypeElement e) {
    Boolean result = Boolean.FALSE;

    /* The current element has contracts on its annotations. */
    result = hasContracts(e.getAnnotationMirrors());

    /* Checks superclass. */
    if (!result) {
      Element superclass =
          utils.typeUtils.asElement(e.getSuperclass());
      if (superclass != null) {
        /* Scans up in the syntax tree looking for contract annotations. */
        result = superclass.accept(this, null);
      }
    }

    /* Checks interfaces. */
    if (!result) {
      Collection<? extends TypeMirror> interfaces = e.getInterfaces();
      for (TypeMirror i : interfaces) {
        Element iface = utils.typeUtils.asElement(i);
        result = iface.accept(this, null);
        if (result) {
          break;
        }
      }
    }

    /* Scan the enclosed elements of this. */
    if (!result) {
      List<? extends Element> enclosed = e.getEnclosedElements();
      for (Element element : enclosed) {
        result = element.accept(this, null);
        if (result) {
          break;
        }
      }
    }

    return result;
  }

  @Requires("annotations != null")
  @Ensures("result != null")
  private Boolean hasContracts(
      Collection<? extends AnnotationMirror> annotations) {
    for (AnnotationMirror a : annotations) {
      if (utils.isContractAnnotation(a)) {
        return Boolean.TRUE;
      }
    }
    return Boolean.FALSE;
  }
}
