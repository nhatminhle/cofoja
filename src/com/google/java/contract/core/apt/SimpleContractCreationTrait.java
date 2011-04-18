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

import com.google.java.contract.core.model.ContractAnnotationModel;
import com.google.java.contract.core.model.VariableModel;

import java.util.Collections;
import java.util.List;

/**
 * A simple contract creation trait that returns the annotation values
 * unmodified.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
class SimpleContractCreationTrait implements ContractCreationTrait {
  protected ContractAnnotationModel annotation;

  @Override
  public boolean visit(ContractAnnotationModel annotation) {
    this.annotation = annotation;
    return true;
  }

  @Override
  public List<String> getExpressions() {
    return annotation.getValues();
  }

  @Override
  public List<String> getMessages() {
    return annotation.getValues();
  }

  @Override
  public List<String> getSourceExpressions() {
    return annotation.getValues();
  }

  @Override
  public List<? extends VariableModel> getInitialParameters() {
    return Collections.emptyList();
  }

  @Override
  public List<? extends VariableModel> getExtraParameters() {
    return Collections.emptyList();
  }

  @Override
  public List<? extends VariableModel> getInitialMockParameters() {
    return getInitialParameters();
  }

  @Override
  public List<? extends VariableModel> getExtraMockParameters() {
    return getExtraParameters();
  }

  @Override
  public String getExceptionName() {
    return "java.lang.AssertionError";
  }
}
