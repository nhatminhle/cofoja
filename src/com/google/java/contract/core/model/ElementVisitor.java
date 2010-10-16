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

import com.google.java.contract.Requires;

/**
 * A visitor of model elements.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
public interface ElementVisitor {
  /**
   * Visits a {@link TypeModel} object.
   */
  @Requires("type != null")
  public void visitType(TypeModel type);

  /**
   * Visits a {@link VariableModel} object.
   */
  @Requires("variable != null")
  public void visitVariable(VariableModel variable);

  /**
   * Visits an {@link MethodModel} object.
   */
  @Requires("method != null")
  public void visitMethod(MethodModel method);

  /**
   * Visits a {@link ContractMethodModel} object.
   */
  @Requires("contract != null")
  public void visitContractMethod(ContractMethodModel contract);

  /**
   * Visits a {@link ContractAnnotationModel} object.
   */
  @Requires("annotation != null")
  public void visitContractAnnotation(ContractAnnotationModel annotation);
}
