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

import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ClassName;
import com.google.java.contract.core.model.ContractAnnotationModel;
import com.google.java.contract.core.model.ContractKind;
import com.google.java.contract.core.model.ContractMethodModel;
import com.google.java.contract.core.model.ElementKind;
import com.google.java.contract.core.model.ElementModifier;
import com.google.java.contract.core.model.MethodModel;
import com.google.java.contract.core.model.TypeModel;
import com.google.java.contract.core.model.TypeName;
import com.google.java.contract.core.model.VariableModel;
import com.google.java.contract.core.util.ElementScanner;
import com.google.java.contract.core.util.JavaUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Element visitor responsible for decorating a {@link TypeModel}
 * object with method-wide contract code elements.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({
  "diagnosticManager != null",
  "preTransformer != null",
  "postTransformer != null",
  "postSignalTransformer != null"
})
public class MethodContractCreator extends ElementScanner {
  /**
   * Creation trait for preconditions.
   */
  protected class PreMethodCreationTrait
      extends ContractExpressionCreationTrait {
    @Requires("transformer != null")
    public PreMethodCreationTrait(
        ContractExpressionTransformer transformer) {
      super(transformer);
    }

    @Override
    public boolean transform(List<String> code, List<Long> lineNumbers,
                             Object sourceInfo) {
      return super.transform(code, lineNumbers, sourceInfo);
    }

    @Override
    public String getExceptionName() {
      return "com.google.java.contract.PreconditionError";
    }
  }

  /**
   * Creation trait for common transformations shared by
   * postconditions and exceptional postconditions.
   */
  protected class CommonPostMethodCreationTrait
      extends ContractExpressionCreationTrait {
    @Requires("transformer != null")
    public CommonPostMethodCreationTrait(
        ContractExpressionTransformer transformer) {
      super(transformer);
    }

    @Override
    public boolean transform(List<String> code, List<Long> lineNumbers,
                              Object sourceInfo) {
      int id = transformer.getNextOldId();
      boolean success = super.transform(code, lineNumbers, sourceInfo);

      if (success) {
        ContractKind oldKind =
            ContractCreation.getContractKind(annotation).getOldKind();
        Iterator<String> iterCode =
            transformer.getOldParametersCode().iterator();
        Iterator<Long> iterLineNumber =
            transformer.getOldParametersLineNumbers().iterator();
        int pos = 0;
        while (iterCode.hasNext()) {
          createOldMethods(oldKind, pos++, id++, iterCode.next(), annotation,
                           iterLineNumber.next());
        }
      }
      return success;
    }

    @Override
    public List<? extends VariableModel> getExtraParameters() {
      return transformer.getOldParameters();
    }

    @Override
    public String getExceptionName() {
      return "com.google.java.contract.PostconditionError";
    }
  }

  /**
   * Creation trait for postconditions.
   */
  protected class PostMethodCreationTrait
      extends CommonPostMethodCreationTrait {
    @Requires("transformer != null")
    public PostMethodCreationTrait(
        ContractExpressionTransformer transformer) {
      super(transformer);
    }

    @Override
    public List<? extends VariableModel> getInitialParameters() {
      if (method.isConstructor()
          || method.getReturnType().getDeclaredName().equals("void")) {
        return Collections.emptyList();
      } else {
        return Collections.singletonList(
            getResultVariable(method.getReturnType()));
      }
    }

    @Override
    public List<? extends VariableModel> getInitialMockParameters() {
      if (method.isConstructor()
          || method.getReturnType().getDeclaredName().equals("void")) {
        return Collections.emptyList();
      } else {
        return Collections.singletonList(
            getResultVariable(annotation.getReturnType()));
      }
    }
  }

  /**
   * Creation trait for exceptional postconditions.
   */
  protected class PostSignalMethodCreationTrait
      extends CommonPostMethodCreationTrait {
    protected List<String> messages;
    protected List<String> sourceCode;

    @Requires("transformer != null")
    public PostSignalMethodCreationTrait(
        ContractExpressionTransformer transformer) {
      super(transformer);
    }

    @Override
    public boolean visit(ContractAnnotationModel annotation) {
      this.annotation = annotation;

      List<String> assocs = annotation.getValues();
      int n = assocs.size() / 2;

      ArrayList<String> code = new ArrayList<String>(n);
      ArrayList<String> msg = new ArrayList<String>(n);
      ArrayList<String> src = new ArrayList<String>(n);
      ArrayList<Long> lines = new ArrayList<Long>(n);

      Iterator<String> it = assocs.iterator();
      Iterator<Long> itLineNumber = annotation.getLineNumbers().iterator();
      try {
        while (it.hasNext()) {
          String exceptionType = it.next();
          String postcondition = it.next();
          code.add("!(signal instanceof " + exceptionType + ") || "
                   + postcondition);
          msg.add(exceptionType + " => " + postcondition);
          src.add(postcondition);

          /*
           * Throw away the line number information of the exception
           * type.
           */
          itLineNumber.next();
          lines.add(itLineNumber.next());
        }
      } catch (NoSuchElementException e) {
        diagnosticManager.warning(
            "extra exception type in "
            + "'com.google.java.contract.ThrowEnsures'; "
            + "ignored",
            assocs.get(assocs.size() - 1), 0, 0, 0,
            annotation.getSourceInfo());
      }

      if (!transform(code, lines, annotation.getSourceInfo())) {
        return false;
      }

      messages = msg;
      sourceCode = src;
      return true;
    }

    @Override
    public List<? extends VariableModel> getInitialParameters() {
      return Collections.singletonList(getSignalVariable());
    }

    @Override
    public List<String> getMessages() {
      return messages;
    }

    @Override
    public List<String> getSourceExpressions() {
      return sourceCode;
    }
  }

  protected DiagnosticManager diagnosticManager;

  protected MethodModel method;
  protected ContractMethodModel preMethod;
  protected ContractMethodModel postMethod;
  protected ContractMethodModel postSignalMethod;

  protected ContractExpressionTransformer preTransformer;
  protected ContractExpressionTransformer postTransformer;
  protected ContractExpressionTransformer postSignalTransformer;

  /**
   * Constructs a new MethodContractCreator.
   */
  @Requires("diagnosticManager != null")
  public MethodContractCreator(DiagnosticManager diagnosticManager) {
    this.diagnosticManager = diagnosticManager;
    method = null;
    preMethod = null;
    postMethod = null;
    postSignalMethod = null;
    preTransformer =
        new ContractExpressionTransformer(diagnosticManager, false);
    postTransformer =
        new ContractExpressionTransformer(diagnosticManager, true);
    postSignalTransformer =
        new ContractExpressionTransformer(diagnosticManager, true);
  }

  @Override
  public void visitMethod(MethodModel method) {
    if (this.method != null) {
      throw new IllegalStateException();
    }
    this.method = method;
    super.visitMethod(method);
  }

  @Override
  public void visitContractAnnotation(ContractAnnotationModel annotation) {
    List<String> code = annotation.getValues();

    if (annotation.getKind().equals(ElementKind.REQUIRES)) {
      PreMethodCreationTrait trait = new PreMethodCreationTrait(preTransformer);
      preMethod = createContractMethods(trait, preMethod, annotation);
    } else if (annotation.getKind().equals(ElementKind.ENSURES)) {
      PostMethodCreationTrait trait =
          new PostMethodCreationTrait(postTransformer);
      postMethod = createContractMethods(trait, postMethod, annotation);
    } else if (annotation.getKind().equals(ElementKind.THROW_ENSURES)) {
      PostSignalMethodCreationTrait trait =
          new PostSignalMethodCreationTrait(postSignalTransformer);
      postSignalMethod = createContractMethods(trait, postSignalMethod,
                                               annotation);
    } else {
      throw new IllegalArgumentException();
    }
  }

  @Requires("type != null")
  @Ensures("result != null")
  private static VariableModel getResultVariable(TypeName type) {
    VariableModel var =
        new VariableModel(ElementKind.PARAMETER, JavaUtils.RESULT_VARIABLE,
                          type);
    var.addModifier(ElementModifier.FINAL);
    return var;
  }

  @Ensures("result != null")
  private static VariableModel getSignalVariable() {
    VariableModel var =
        new VariableModel(ElementKind.PARAMETER, JavaUtils.SIGNAL_VARIABLE,
                          new ClassName("java/lang/Exception"));
    var.addModifier(ElementModifier.FINAL);
    return var;
  }

  /**
   * Creates contract and helper methods according to the parameters,
   * and adds it to the parent type.
   *
   * @param kind the kind of contract method to create
   * @param pos the relative position of {@code expr} in its
   * annotation
   * @param id the contract method ID
   * @param expr the expression computing the old value
   * @param annotation the annotation value from which this contract
   * is created
   */
  @Requires({
    "kind != null",
    "pos >= 0",
    "id >= 0",
    "pos <= id",
    "expr != null",
    "annotation != null",
    "kind.isOld()",
    "lineNumber == null || lineNumber >= 1"
  })
  private void createOldMethods(ContractKind kind,
      int pos, int id, String expr, ContractAnnotationModel annotation,
      Long lineNumber) {
    MethodModel helper =
        ContractCreation.createBlankContractHelper(kind, annotation,
                                                   "$" + Integer.toString(pos));
    helper.setReturnType(new ClassName("java/lang/Object"));

    if (helper.getKind() == ElementKind.CONTRACT_METHOD) {
      ContractMethodModel helperContract = (ContractMethodModel) helper;
      if (lineNumber != null) {
        helperContract.setLineNumbers(Collections.singletonList(lineNumber));
      }
      String code = expr;
      if (!annotation.isVirtual()) {
        code = ContractCreation
            .rebaseLocalCalls(expr, JavaUtils.THAT_VARIABLE, null);
      }
      helperContract.addStatement("return " + code + ";");
    }

    ContractMethodModel contract =
        ContractCreation.createBlankContractMethod(kind, annotation, "$" + id);
    contract.setReturnType(new ClassName("java/lang/Object"));
    contract.setId(id);

    contract.addStatement("return "
        + ContractCreation.getHelperCallCode(helper, annotation) + ";");
  }
}
