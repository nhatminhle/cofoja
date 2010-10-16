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

import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ContractAnnotationModel;

import java.util.List;

/**
 * Creation trait for common transformations shared by all kinds of
 * contracts.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant("transformer != null")
class ContractExpressionCreationTrait
    extends SimpleContractCreationTrait {
  protected ContractExpressionTransformer transformer;

  /**
   * Constructs a new ContractExpressionTransformer using
   * {@code transformer}.
   */
  @Requires("transformer != null")
  ContractExpressionCreationTrait(
      ContractExpressionTransformer transformer) {
    this.transformer = transformer;
  }

  /**
   * Checks and transforms {@code code} through {@link #transformer}.
   *
   * @see ContractExpressionTransformer#transform(List<String>,List<Long>,Object)
   */
  @Requires({
    "code != null",
    "lineNumbers != null",
    "code.size() == lineNumbers.size()"
  })
  protected boolean transform(List<String> code, List<Long> lineNumbers,
                              Object sourceInfo) {
    return transformer.transform(code, lineNumbers, sourceInfo);
  }

  @Override
  public boolean visit(ContractAnnotationModel annotation) {
    this.annotation = annotation;
    return transform(annotation.getValues(), annotation.getLineNumbers(),
                     annotation.getSourceInfo());
  }

  @Override
  public List<String> getExpressions() {
    return transformer.getTransformedCode();
  }
}
