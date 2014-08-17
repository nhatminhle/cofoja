/*
 * Copyright 2010 Google Inc.
 * Copyright 2011 Nhat Minh Lê
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
import com.google.java.contract.core.model.ClassName;
import com.google.java.contract.core.model.ContractAnnotationModel;
import com.google.java.contract.core.model.ContractKind;
import com.google.java.contract.core.model.ContractMethodModel;
import com.google.java.contract.core.model.ContractVariance;
import com.google.java.contract.core.model.ElementKind;
import com.google.java.contract.core.model.ElementModifier;
import com.google.java.contract.core.model.MethodModel;
import com.google.java.contract.core.model.TypeModel;
import com.google.java.contract.core.model.TypeName;
import com.google.java.contract.core.model.VariableModel;
import com.google.java.contract.core.util.Elements;
import com.google.java.contract.core.util.JavaTokenizer.Token;
import com.google.java.contract.core.util.JavaTokenizer.TokenKind;
import com.google.java.contract.core.util.JavaUtils;
import com.google.java.contract.core.util.PushbackTokenizer;

import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Utility methods for contract creators.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@ContractImport("com.google.java.contract.core.model.ClassName")
public class ContractCreation {
  static final String RAISE_METHOD =
      "com.google.java.contract.core.runtime.ContractRuntime.raise";

  /**
   * Returns {@code code} with all unqualified or this-qualified
   * identifiers followed by an open parenthesis rebased to
   * {@code that}. Unqualified identifiers in {@code whitelist}
   * are not subject to this change.
   */
  @Requires({
    "code != null",
    "that != null"
  })
  @Ensures("result != null")
  static String rebaseLocalCalls(String code, String that,
                                 Set<String> whitelist) {
    StringBuilder buffer = new StringBuilder();
    PushbackTokenizer tokenizer = new PushbackTokenizer(new StringReader(code));
    boolean qualified = false;
    while (tokenizer.hasNext()) {
      Token token = tokenizer.next();
      if (!qualified && token.kind == TokenKind.WORD
          && (whitelist == null || !whitelist.contains(token.text))) {
        if (token.text.equals("this")) {
          buffer.append("( ");
          buffer.append(JavaUtils.BEGIN_GENERATED_CODE);
          buffer.append(that);
          buffer.append(JavaUtils.END_GENERATED_CODE);
          buffer.append(" )");
        } else {
          if (JavaUtils.lookingAt(tokenizer, "(")) {
            buffer.append(JavaUtils.BEGIN_GENERATED_CODE);
            buffer.append(that);
            buffer.append(".");
            buffer.append(JavaUtils.END_GENERATED_CODE);
            buffer.append(token.text);
          } else {
            buffer.append(token.text);
          }
        }
      } else {
        buffer.append(token.text);
      }
      qualified = token.text.equals(".");
    }
    return buffer.toString();
  }

  /**
   * Builds a contract method body from the {@code trait}.
   *
   * @param contract the contract method to add the clauses to
   * @param trait the trait to get contract code information from
   * @param annotation the source of the contract
   */
  @Requires({
    "contract != null",
    "trait != null",
    "annotation != null"
  })
  static void addContractClauses(ContractMethodModel contract,
                                 ContractCreationTrait trait,
                                 ContractAnnotationModel annotation) {
    ContractKind kind = getContractKind(annotation);

    Iterator<String> itCode = trait.getExpressions().iterator();
    Iterator<String> itMsg = trait.getMessages().iterator();
    Iterator<String> itComment = trait.getSourceExpressions().iterator();
    int successVariableCount = 0;
    int exceptionVariableCount = 0;
    while (itCode.hasNext()) {
      StringBuilder buffer = new StringBuilder();
      String expr = itCode.next();
      String exprMsg = itMsg.next();
      String exprComment = itComment.next();

      /*
       * Evaluate predicate. The success variable is first assigned a
       * dummy value so as to be able to track the start of the
       * evaluation in the generated bytecode.
       */
      String successVariableName =
          JavaUtils.SUCCESS_VARIABLE_PREFIX + "$" + successVariableCount++;
      String exceptionTempVariableName =
          JavaUtils.EXCEPTION_VARIABLE_PREFIX + "$" + exceptionVariableCount++;
      String exceptionVariableName =
          JavaUtils.EXCEPTION_VARIABLE_PREFIX + "$" + exceptionVariableCount++;
      buffer.append("boolean ");
      buffer.append(successVariableName);
      buffer.append(" = false; ");
      buffer.append("Throwable ");
      buffer.append(exceptionVariableName);
      buffer.append(" = null; ");
      buffer.append("try { ");
      buffer.append(successVariableName);
      buffer.append(" = ");
      buffer.append(JavaUtils.BEGIN_LOCATION_COMMENT);
      buffer.append(JavaUtils.quoteComment(exprComment));
      buffer.append(JavaUtils.END_LOCATION_COMMENT);
      if (!annotation.isVirtual()) {
        buffer.append(rebaseLocalCalls(expr, JavaUtils.THAT_VARIABLE, null));
      } else {
        buffer.append(expr);
      }
      buffer.append("; ");
      buffer.append("} catch(Throwable ");
      buffer.append(exceptionTempVariableName);
      buffer.append(") {");
      buffer.append(exceptionVariableName);
      buffer.append(" = ");
      buffer.append(exceptionTempVariableName);
      buffer.append("; } ");

      /* Handle failure. */
      buffer.append("if (!(");
      buffer.append(successVariableName);
      buffer.append(")) { ");
      if (kind.getVariance() == ContractVariance.CONTRAVARIANT) {
        buffer.append("return new ");
        buffer.append(trait.getExceptionName());
        buffer.append("(\"");
        buffer.append(ContractWriter.quoteString(exprMsg));
        buffer.append("\", ");
        buffer.append(JavaUtils.ERROR_VARIABLE);
        buffer.append(", ");
        buffer.append(exceptionVariableName);
        buffer.append("); ");
      } else {
        buffer.append(RAISE_METHOD);
        buffer.append("(new ");
        buffer.append(trait.getExceptionName());
        buffer.append("(\"");
        buffer.append(ContractWriter.quoteString(exprMsg));
        buffer.append("\", ");
        buffer.append(exceptionVariableName);
        buffer.append("));");
      }
      buffer.append("} ");

      contract.addStatement(buffer.toString());
    }
  }

  /**
   * Builds a contract method body that calls the specified helper
   * contract method.
   */
  @Requires({
    "helper != null",
    "annotation != null"
  })
  static String getHelperCallCode(MethodModel helper,
                                  ContractAnnotationModel annotation) {
    StringBuilder buffer = new StringBuilder();
    if (!annotation.isPrimary() && !annotation.isVirtual()) {
      buffer.append(annotation.getOwner().getQualifiedName()
                    + JavaUtils.HELPER_CLASS_SUFFIX);
      buffer.append(".");
    }
    buffer.append(helper.getSimpleName());
    buffer.append("(");
    List<? extends VariableModel> parameters = helper.getParameters();
    if (!parameters.isEmpty()) {
      Iterator<? extends VariableModel> it = parameters.iterator();
      for (;;) {
        String name = it.next().getSimpleName();
        if (name.equals(JavaUtils.THAT_VARIABLE)) {
          buffer.append("this");
        } else {
          buffer.append(name);
        }
        if (!it.hasNext()) {
          break;
        }
        buffer.append(", ");
      }
    }
    buffer.append(")");
    return buffer.toString();
  }

  static ContractKind getContractKind(ContractAnnotationModel annotation) {
    switch (annotation.getKind()) {
      case INVARIANT:
        return ContractKind.INVARIANT;
      case REQUIRES:
        return ContractKind.PRE;
      case ENSURES:
        return ContractKind.POST;
      case THROW_ENSURES:
        return ContractKind.SIGNAL;
      default:
        throw new IllegalArgumentException();
    }
  }

  /**
   * Visits the specified trait and creates or augment contract and
   * helper methods as needed.
   *
   * @see #createContractMethod(ContractCreationTrait,ContractMethodModel,ContractAnnotationModel,MethodModel)
   * @see #createContractHelper(ContractCreationTrait,ContractAnnotationModel)
   */
  @Requires({
    "trait != null",
    "annotation != null"
  })
  static ContractMethodModel createContractMethods(
      ContractCreationTrait trait, ContractMethodModel contract,
      ContractAnnotationModel annotation) {
    if (!trait.visit(annotation)) {
      return null;
    }
    MethodModel helper = createContractHelper(trait, annotation);
    return createContractMethod(trait, contract, annotation, helper);
  }

  /**
   * Returns a new blank contract method, according to the annotation
   * properties, and adds it to the parent type. Such a method returns
   * {@code void}, and has no code and no line information.
   *
   * @param kind the kind of contract method to create
   * @param annotation the source of the contract
   * @param nameSuffix a suffix to append to the method name, or
   * {@code null}
   * @return the contract method
   */
  @Requires({
    "kind != null",
    "annotation != null"
  })
  @Ensures("result != null")
  static ContractMethodModel createBlankContractMethod(
      ContractKind kind, ContractAnnotationModel annotation,
      String nameSuffix) {
    TypeModel type = Elements.getTypeOf(annotation);

    MethodModel contracted = null;
    if (kind.isMethodContract()) {
      contracted = (MethodModel) annotation.getEnclosingElement();
    }

    String name = kind.getNameSpace() + getContractName(kind, contracted);
    if (nameSuffix != null) {
      name += nameSuffix;
    }
    ContractMethodModel contract =
        new ContractMethodModel(kind, name, new TypeName("void"), contracted);

    contract.addModifier(ElementModifier.PRIVATE);
    type.addMember(contract);

    return contract;
  }

  /**
   * Adds to the specified contract method, or creates a new one and
   * adds it to the parent type.
   *
   * <p>This method does <em>not</em> visit its trait argument.
   *
   * @param trait the trait object used to create the contract
   * @param contract the contract object to be augmented,
   * or {@code null}
   * @param annotation the source of the contract
   * @param helper a helper method to call or {@code null} if this
   * contract is direct
   * @return the contract method
   */
  @Requires({
    "trait != null",
    "annotation != null",
    "helper != null"
  })
  @Ensures("result != null")
  static ContractMethodModel createContractMethod(
      ContractCreationTrait trait, ContractMethodModel contract,
      ContractAnnotationModel annotation, MethodModel helper) {
    ContractKind kind = getContractKind(annotation);

    if (contract == null) {
      contract = createBlankContractMethod(kind, annotation, "");
      Elements.copyParameters(contract, trait.getInitialParameters());

      if (kind.getVariance() == ContractVariance.CONTRAVARIANT) {
        contract.setPrologue(trait.getExceptionName() + " "
                             + JavaUtils.ERROR_VARIABLE + " = null;");
        contract.setEpilogue(RAISE_METHOD + "("
                             + JavaUtils.ERROR_VARIABLE + ");");
      }
    }
    Elements.copyParameters(contract, trait.getExtraParameters());

    if (annotation.isPrimary()) {
      contract.setSourceInfo(annotation.getSourceInfo());
    }

    String code = getHelperCallCode(helper, annotation) + ";";
    if (kind.getVariance() == ContractVariance.CONTRAVARIANT) {
      code = JavaUtils.ERROR_VARIABLE + " = " + code
          + "if (" + JavaUtils.ERROR_VARIABLE + " == null) { return; }";
    }
    contract.addStatement(code);

    return contract;
  }

  /**
   * Returns a new blank primary or mock helper contract method,
   * according to the annotation properties, and adds it to the parent
   * type if needed. Such a method returns {@code void} and has no
   * code and no line information.
   *
   * @param kind the kind of contract method to create
   * @param annotation the source of the contract
   * @param nameSuffix a suffix to append to the method name, or
   * {@code null}
   */
  @Requires({
    "kind != null",
    "annotation != null"
  })
  @Ensures("result != null")
  static MethodModel createBlankContractHelper(
      ContractKind kind, ContractAnnotationModel annotation,
      String nameSuffix) {
    TypeModel type = Elements.getTypeOf(annotation);
    MethodModel method = null;
    ContractMethodModel contract = null;

    MethodModel contracted = null;
    if (kind.isMethodContract()) {
      contracted = (MethodModel) annotation.getEnclosingElement();
    }

    TypeName returnType = new TypeName("void");
    String name = getHelperName(kind, annotation.getOwner(), contracted);
    if (nameSuffix != null) {
      name += nameSuffix;
    }
    if (annotation.isPrimary()) {
      contract = new ContractMethodModel(ContractKind.HELPER, name,
                                         returnType, contracted);

      contract.setSourceInfo(annotation.getSourceInfo());

      if (!annotation.isVirtual()) {
        for (TypeName typeParam : type.getTypeParameters()) {
          contract.addTypeParameter(typeParam);
        }
      }

      method = contract;
    } else {
      method = new MethodModel(ElementKind.CONTRACT_MOCK, name, returnType);
      if (contracted != null) {
        Elements.copyParameters(method, contracted.getParameters());
      }
    }

    if (!annotation.isVirtual()) {
      method.addParameter(
          new VariableModel(ElementKind.PARAMETER,
                            JavaUtils.THAT_VARIABLE, annotation.getOwner()));
    }

    if (!annotation.isVirtual()) {
      method.addModifier(ElementModifier.PUBLIC);
      method.addModifier(ElementModifier.STATIC);
    } else {
      method.addModifier(ElementModifier.PROTECTED);
    }

    if (annotation.isPrimary()
        || annotation.isVirtual() && !annotation.isWeakVirtual()) {
      type.addMember(method);
    }

    return method;
  }

  /**
   * Creates a new primary or mock helper contract method, according
   * to the annotation properties, and adds it to the parent type if
   * needed.
   *
   * <p>This method does <em>not</em> visit its trait argument.
   *
   * @param trait the trait object used to create the contract
   * @param annotation the source of the contract
   */
  @Requires({
    "trait != null",
    "annotation != null"
  })
  @Ensures("result != null")
  static MethodModel createContractHelper(ContractCreationTrait trait,
                                          ContractAnnotationModel annotation) {
    ContractKind kind = getContractKind(annotation);
    MethodModel method = createBlankContractHelper(kind, annotation, null);

    TypeName returnType =
        new TypeName(kind.getVariance() == ContractVariance.CONTRAVARIANT
                     ? trait.getExceptionName()
                     : "void");
    method.setReturnType(returnType);

    if (kind.getVariance() == ContractVariance.CONTRAVARIANT) {
      method.addParameter(
          new VariableModel(ElementKind.PARAMETER,
                            JavaUtils.ERROR_VARIABLE, returnType));
    }

    if (annotation.isPrimary()) {
      method.setSourceInfo(annotation.getSourceInfo());
    }

    if (method.getKind() == ElementKind.CONTRACT_METHOD) {
      ContractMethodModel contract = (ContractMethodModel) method;

      Elements.copyParameters(contract, trait.getInitialParameters());
      Elements.copyParameters(contract, trait.getExtraParameters());

      addContractClauses(contract, trait, annotation);
      if (kind.getVariance() == ContractVariance.CONTRAVARIANT) {
        contract.setEpilogue("return null;");
      }

      if (annotation.isPrimary()) {
        contract.setLineNumbers(annotation.getLineNumbers());
      }
    } else {
      Elements.copyParameters(method, trait.getInitialMockParameters());
      Elements.copyParameters(method, trait.getExtraMockParameters());
    }

    return method;
  }

  /**
   * Builds and returns a helper method name.
   */
  @Requires({
    "kind != null",
    "owner != null"
  })
  @Ensures("ClassName.isSimpleName(result)")
  static String getHelperName(ContractKind kind, ClassName owner,
                              MethodModel contracted) {
    return kind.getHelperNameSpace() + "$"
        + owner.getBinaryName().replace('/', '$')
        + getContractName(kind, contracted);
  }

  /**
   * Returns the relative contract name, without the name space part.
   */
  @Requires({
    "kind != null",
    "!kind.isClassContract() || contracted == null"
  })
  @Ensures({
    "result.isEmpty() " +
        "|| ClassName.isSimpleName(result) && result.startsWith(\"$\")"
  })
  static String getContractName(ContractKind kind, MethodModel contracted) {
    if (contracted == null) {
      return "";
    } else {
      if (contracted.isConstructor()) {
        return "$" + contracted.getEnclosingElement().getSimpleName();
      } else {
        return "$" + contracted.getSimpleName();
      }
    }
  }
}
