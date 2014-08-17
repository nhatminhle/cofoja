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
package com.google.java.contract.core.apt;

import com.google.java.contract.ContractImport;
import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ContractAnnotationModel;
import com.google.java.contract.core.model.VariableModel;

import java.util.List;

/**
 * A hybrid visitor with query methods that governs the creation of a
 * given kind of contract.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@ContractImport("com.google.java.contract.core.model.ClassName")
interface ContractCreationTrait {
  /**
   * Called before any of the query methods of this object. Queries
   * between two calls to this method should be consistent.
   *
   * @param annotation the annotation to process
   * @return {@code true} if there was no error
   */
  @Requires("annotation != null")
  public boolean visit(ContractAnnotationModel annotation);

  /**
   * Returns the list of expressions to be evaluated by the contract.
   */
  @Ensures("result != null")
  public List<String> getExpressions();

  /**
   * Returns the list of failure messages associated with the
   * assertions of the contract.
   */
  @Ensures("result != null")
  public List<String> getMessages();

  /**
   * Returns the list of expressions to be used in error
   * reporting. This should be a list of unchanged clauses from the
   * original annotation.
   */
  @Ensures("result != null")
  public List<String> getSourceExpressions();

  /**
   * Returns the list of extra parameters to add to the end of the
   * contract method parameter list. These parameters are only added
   * at creation; they are <em>not</em> added to an existing contract
   * method.
   */
  @Ensures("result != null")
  public List<? extends VariableModel> getInitialParameters();

  /**
   * Returns the list of extra parameters to add to the end of the
   * contract method parameter list. These parameters are added to the
   * contract method regardless of whether it is new or not, after
   * parameters returned by
   * {@link #getInitialParameters(ContractAnnotationModel)},
   * if applicable.
   */
  @Ensures("result != null")
  public List<? extends VariableModel> getExtraParameters();

  /**
   * Returns the list of initial extra parameters for mock
   * definitions.
   *
   * @see getInitialParameters()
   */
  @Ensures("result != null")
  public List<? extends VariableModel> getInitialMockParameters();

  /**
   * Returns the list of extra parameters for mock definitions.
   *
   * @see getExtraParameters()
   */
  @Ensures("result != null")
  public List<? extends VariableModel> getExtraMockParameters();

  /**
   * Returns the name of the exception type to throw when a failure
   * occurs.
   */
  @Ensures("ClassName.isQualifiedName(result)")
  public String getExceptionName();
}
