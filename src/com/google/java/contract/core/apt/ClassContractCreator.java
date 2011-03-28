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
package com.google.java.contract.core.apt;

import static com.google.java.contract.core.apt.ContractCreation.createContractMethods;

import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ContractAnnotationModel;
import com.google.java.contract.core.model.ContractMethodModel;
import com.google.java.contract.core.model.ElementKind;
import com.google.java.contract.core.model.HelperTypeModel;
import com.google.java.contract.core.model.MethodModel;
import com.google.java.contract.core.model.TypeModel;
import com.google.java.contract.core.util.ElementScanner;

/**
 * Element visitor responsible for decorating a {@link TypeModel}
 * object with class-wide contract code elements. Delegates
 * method-wide contracts to {@link MethodContractCreator}.
 *
 * <p>Contracts are combined together into a single contract method,
 * which evaluates the contracts. Moreover, each primary annotation
 * also creates a helper method called a primary contract method.
 *
 * <p>A primary contract method only evaluates the current class
 * invariant, ignoring inherited contracts. It is called by children
 * of the class to work around access restrictions.
 *
 * <p>Interface contracts do not have a primary implementation as
 * interfaces have no code body of their own. Since there can also
 * be no access restrictions in interface contracts, they are simply
 * inlined.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({
  "diagnosticManager != null",
  "transformer != null"
})
public class ClassContractCreator extends ElementScanner {
  protected DiagnosticManager diagnosticManager;

  protected TypeModel type;
  protected TypeModel helperType;
  protected ContractMethodModel invariant;

  protected ContractExpressionTransformer transformer;

  /**
   * Constructs a new ClassContractCreator.
   */
  @Requires("diagnosticManager != null")
  public ClassContractCreator(DiagnosticManager diagnosticManager) {
    this.diagnosticManager = diagnosticManager;
    type = null;
    helperType = null;
    invariant = null;
    transformer = new ContractExpressionTransformer(diagnosticManager, false);
  }

  public TypeModel getHelperType() {
    return helperType;
  }

  @Override
  public void visitType(TypeModel type) {
    if (this.type != null) {
      ClassContractCreator creator =
          new ClassContractCreator(diagnosticManager);
      type.accept(creator);
      if (creator.getHelperType() != null) {
        this.type.addEnclosedElement(creator.getHelperType());
      }
      return;
    }
    this.type = type;
    if (type.getKind() == ElementKind.INTERFACE) {
      helperType = new HelperTypeModel(type);
      super.visitType(helperType);
    } else {
      super.visitType(type);
    }
  }

  @Override
  public void visitMethod(MethodModel method) {
    method.accept(new MethodContractCreator(diagnosticManager));
  }

  @Override
  public void visitContractAnnotation(ContractAnnotationModel annotation) {
    if (!annotation.getKind().equals(ElementKind.INVARIANT)) {
      throw new IllegalArgumentException();
    }
    ContractExpressionCreationTrait trait =
        new ContractExpressionCreationTrait(transformer) {
          @Override
          public String getExceptionName() {
            return "com.google.java.contract.InvariantError";
          }
        };
    invariant = createContractMethods(trait, invariant, annotation);
  }
}
