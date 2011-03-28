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
package com.google.java.contract.core.util;

import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ContractAnnotationModel;
import com.google.java.contract.core.model.ContractMethodModel;
import com.google.java.contract.core.model.ElementModel;
import com.google.java.contract.core.model.MethodModel;
import com.google.java.contract.core.model.TypeModel;
import com.google.java.contract.core.model.VariableModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A scanner that recursively descends into an {@link Element}.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
public abstract class ElementScanner extends EmptyElementVisitor {
  @Requires({
    "elements != null",
    "!elements.contains(null)"
  })
  public void scan(List<? extends ElementModel> elements) {
    ArrayList<? extends ElementModel> tmp =
        new ArrayList<ElementModel>(elements);
    for (ElementModel element : tmp) {
      element.accept(this);
    }
  }

  @Override
  public void visitType(TypeModel type) {
    scan(type.getEnclosedElements());
  }

  @Override
  public void visitVariable(VariableModel variable) {
    scan(variable.getEnclosedElements());
  }

  @Override
  public void visitMethod(MethodModel method) {
    scan(method.getEnclosedElements());
  }

  @Override
  public void visitContractMethod(ContractMethodModel contract) {
    scan(contract.getEnclosedElements());
  }

  @Override
  public void visitContractAnnotation(ContractAnnotationModel annotation) {
    scan(annotation.getEnclosedElements());
  }
}
