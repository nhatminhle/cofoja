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
import com.google.java.contract.Invariant;
import com.google.java.contract.core.model.ContractKind;
import com.google.java.contract.core.util.DebugUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Bytecode class visitor. Initiates/delegates method instrumentation
 * to instances of {@link SpecificationMethodAdapter}.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
@ContractImport("com.google.java.contract.core.model.ClassName")
@Invariant({
  "getClassName() == null || ClassName.isBinaryName(getClassName())",
  "getContracts() != null",
  "getParent() != null"
})
class SpecificationClassAdapter extends ClassVisitor {
  protected String className;
  protected ContractAnalyzer contracts;

  public SpecificationClassAdapter(ClassVisitor cv,
                                   ContractAnalyzer contracts) {
    super(Opcodes.ASM5, cv);
    this.contracts = contracts;
  }

  @Override
  public void visit(int version, int access, String name, String signature,
                    String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    className = name;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
                                   String signature, String[] exceptions) {
    MethodVisitor mv =
        cv.visitMethod(access, name, desc, signature, exceptions);
    if ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
      return mv;
    }

    return new SpecificationMethodAdapter(this, mv, access, name, desc);
  }

  @Override
  public void visitEnd() {
    if (contracts != null) {
      List<ClassContractHandle> synths = new ArrayList<ClassContractHandle>();
      synths.addAll(contracts.getClassHandles(ContractKind.ACCESS));
      synths.addAll(contracts.getClassHandles(ContractKind.LAMBDA));
      for (ClassContractHandle h : synths) {
        h.getContractMethod().accept(cv);
      }

      List<ClassContractHandle> helpers =
          contracts.getClassHandles(ContractKind.HELPER);
      for (ClassContractHandle h : helpers) {
        MethodNode methodNode = h.getContractMethod();
        DebugUtils.info("instrument", "helper method "
                        + className + "." + methodNode.name
                        + methodNode.desc);
        ClassVisitor visitor = cv;
        List<Long> lineNumbers = h.getLineNumbers();
        if (lineNumbers != null) {
          visitor = new LineNumberingClassAdapter(visitor, lineNumbers);
        }
        methodNode.accept(new ContractFixingClassAdapter(visitor));
        h.setInjected(true);
      }
    }
    super.visitEnd();
  }

  /**
   * Returns the name of the visited class.
   */
  String getClassName() {
    return className;
  }

  /**
   * Returns the class visitor this one delegates to.
   */
  ClassVisitor getParent() {
    return cv;
  }

  /**
   * Returns the contract analyzer of the visited class.
   */
  ContractAnalyzer getContracts() {
    return contracts;
  }
}
