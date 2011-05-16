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
import java.util.List;

/**
 * A model element representing a contract method. Contrary to other
 * methods, contract methods are not mocked and the model accounts for
 * the implementation code. This code is stored in text form.
 */
@Invariant({
  "getContractKind() != null",
  "getId() >= -1",
  "getStatements() != null",
  "!getStatements().contains(null)",
  "!getContractKind().isClassContract() || getContractedMethod() == null",
  "!getContractKind().isMethodContract() || getContractedMethod() != null"
})
public class ContractMethodModel extends MethodModel {
  private static final ClassName CONTRACT_SIGNATURE_CLASS =
      new ClassName("com/google/java/contract/core/ContractMethodSignature");

  /**
   * The kind of contract this method implements.
   */
  protected ContractKind contractKind;

  /**
   * The ID of this contract method.
   */
  protected int id;

  /**
   * The body of this method, as strings of code.
   */
  protected List<String> statements;

  /**
   * A fixed prologue.
   */
  protected String prologue;

  /**
   * A fixed epilogue.
   */
  protected String epilogue;

  /**
   * The contracted method this contract applies to, or {@code null}
   * if this is a class-wide contract.
   */
  protected MethodModel contractedMethod;

  /**
   * The line numbers associated with the original source annotation
   * that specifies this contract.
   */
  protected List<Long> lineNumbers;

  /**
   * Constructs a new ContractMethodModel.
   *
   * @param kind the kind of this contract
   * @param name the name of this contract method
   * @param returnType the return type of this method
   * @param contracted the contracted method, if any
   */
  @Requires({
    "kind != null",
    "name != null",
    "returnType != null",
    "!kind.isClassContract() || contracted == null"
  })
  public ContractMethodModel(ContractKind kind, String name,
                             TypeName returnType, MethodModel contracted) {
    super(ElementKind.CONTRACT_METHOD, name, returnType);

    if (contracted != null) {
      typeParameters = new ArrayList<TypeName>(contracted.getTypeParameters());
      Elements.copyParameters(this, contracted.getParameters());
      modifiers = EnumSet.copyOf(contracted.getModifiers());
    }
    fixCommonModifiers();

    contractKind = kind;
    id = -1;

    statements = new ArrayList<String>();
    prologue = null;
    epilogue = null;

    contractedMethod = contracted;
    lineNumbers = null;
  }

  /**
   * Constructs a clone of {@code that}. The new object is identical
   * to the original <em>except</em> it has no enclosing element.
   */
  @Requires("that != null")
  @Ensures("getEnclosingElement() == null")
  public ContractMethodModel(ContractMethodModel that) {
    super(that);

    contractKind = that.contractKind;
    id = that.id;

    statements = new ArrayList<String>(that.statements);
    prologue = that.prologue;
    epilogue = that.epilogue;

    contractedMethod = that.contractedMethod;
    lineNumbers = that.lineNumbers;
  }

  @Override
  public ContractMethodModel clone() {
    return new ContractMethodModel(this);
  }

  /**
   * Removes commonly undesirable modifiers that might have been
   * inherited.
   */
  private void fixCommonModifiers() {
    removeModifier(ElementModifier.ABSTRACT);
    removeModifier(ElementModifier.TRANSIENT);
  }

  public ContractKind getContractKind() {
    return contractKind;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public MethodModel getContractedMethod() {
    return contractedMethod;
  }

  public List<Long> getLineNumbers() {
    return lineNumbers;
  }

  @Ensures("lineNumbers == getLineNumbers()")
  public void setLineNumbers(List<Long> lineNumbers) {
    this.lineNumbers = lineNumbers;
  }

  @Ensures("result != null")
  public String getCode() {
    StringBuilder buffer = new StringBuilder();
    if (prologue != null) {
      buffer.append(prologue);
    }
    for (String stmt : statements) {
      buffer.append(stmt);
    }
    if (epilogue != null) {
      buffer.append(epilogue);
    }
    return buffer.toString();
  }

  public List<String> getStatements() {
    return Collections.unmodifiableList(statements);
  }

  public String getPrologue() {
    return prologue;
  }

  @Ensures("getPrologue().equals(prologue)")
  public void setPrologue(String prologue) {
    this.prologue = prologue;
  }

  public String getEpilogue() {
    return epilogue;
  }

  @Ensures("getEpilogue().equals(epilogue)")
  public void setEpilogue(String epilogue) {
    this.epilogue = epilogue;
  }

  @Ensures("getStatements().isEmpty()")
  public void clearStatements() {
    statements.clear();
  }

  @Requires("stmt != null")
  @Ensures({
    "getStatements().size() == old(getStatements().size()) + 1",
    "getStatements().contains(stmt)"
  })
  public void addStatement(String stmt) {
    statements.add(stmt);
  }

  @Override
  public void accept(ElementVisitor visitor) {
    visitor.visitContractMethod(this);
  }

  @Override
  public EnumSet<ElementKind> getAllowedEnclosedKinds() {
    EnumSet<ElementKind> allowed =
        EnumSet.of(ElementKind.CONTRACT_SIGNATURE);
    allowed.addAll(super.getAllowedEnclosedKinds());
    return allowed;
  }
}
