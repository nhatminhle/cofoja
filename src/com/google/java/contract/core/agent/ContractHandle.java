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
package com.google.java.contract.core.agent;

import com.google.java.contract.ContractImport;
import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ContractKind;
import com.google.java.contract.core.util.JavaUtils;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * A handle representing a contract method at run time. The handle
 * gives access to the contract method as well as metadata required
 * to instrument the elements it targets.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
@ContractImport({
  "com.google.java.contract.core.model.ClassName",
  "com.google.java.contract.util.Iterables",
  "com.google.java.contract.util.Predicates"
})
@Invariant({
  "getKind() != null",
  "ClassName.isBinaryName(getClassName())",
  "getKey() >= -1",
  "getLineNumbers() == null " +
      "|| ContractMethodSignatures.isLineNumberList(getLineNumbers())"
})
public class ContractHandle {
  protected ContractKind kind;
  protected String className;
  protected int key;

  protected MethodNode contractMethod;
  protected List<Long> lineNumbers;

  protected boolean injected;

  /**
   * Constructs a new ContractHandle.
   *
   * @param kind the kind of the contract handle
   * @param className the name of the contracted class
   * @param contractMethod the method node holding the actual
   * implementation of the contract method
   * @param lineNumbers the line numbers associated with the contract
   */
  @Requires({
    "kind != null",
    "ClassName.isBinaryName(className)",
    "contractMethod != null",
    "lineNumbers == null " +
        "|| ContractMethodSignatures.isLineNumberList(lineNumbers)"
  })
  @Ensures({
    "kind == getKind()",
    "className.equals(getClassName())",
    "contractMethod == getContractMethod()",
    "lineNumbers == getLineNumbers()",
    "!isInjected()"
  })
  protected ContractHandle(ContractKind kind, String className,
                           MethodNode contractMethod, List<Long> lineNumbers) {
    this.kind = kind;
    this.className = className;
    key = ContractMethodSignatures.getId(contractMethod);

    this.contractMethod = contractMethod;
    if (!contractMethod.name.startsWith("com$google$java$contract$")) {
      contractMethod.name =
          JavaUtils.SYNTHETIC_MEMBER_PREFIX + contractMethod.name;
    }
    this.lineNumbers = lineNumbers;

    this.injected = false;
  }

  public ContractKind getKind() {
    return kind;
  }

  public String getClassName() {
    return className;
  }

  public int getKey() {
    return key;
  }

  public MethodNode getContractMethod() {
    return contractMethod;
  }

  public List<Long> getLineNumbers() {
    return lineNumbers;
  }

  public boolean isInjected() {
    return injected;
  }

  @Ensures("injected == isInjected()")
  public void setInjected(boolean injected) {
    this.injected = injected;
  }
}
