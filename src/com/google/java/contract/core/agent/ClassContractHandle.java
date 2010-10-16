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
package com.google.java.contract.core.agent;

import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ContractKind;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * A contract handle representing a contract method with class-wide
 * effect.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class ClassContractHandle extends ContractHandle {
  /**
   * Constructs a new ClassContractHandle.
   *
   * @param kind the kind of this ClassContractHandle
   * @param className the class this handle belongs to
   * @param contractMethod the {@link MethodNode} representing this
   * handle's method
   * @param lineNumbers the line numbers associated with the contract
   */
  @Requires({
    "kind != null",
    "className != null",
    "contractMethod != null"
  })
  @Ensures({
    "kind == getKind()",
    "className.equals(getClassName())",
    "contractMethod == getContractMethod()",
    "lineNumbers == getLineNumbers()"
  })
  public ClassContractHandle(ContractKind kind, String className,
                             MethodNode contractMethod,
                             List<Long> lineNumbers) {
    super(kind, className, contractMethod, lineNumbers);
  }
}
