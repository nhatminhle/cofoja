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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bytecode class visitor responsible for contract information
 * extraction.
 *
 * After a class has been visited, this visitor exposes the resulting
 * handles through filtering accessor methods such as
 * {@link #getClassHandles(ContractKind)}.
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
  "className == null || ClassName.isBinaryName(className)",
  "classHandles != null",
  "!classHandles.contains(null)",
  "methodHandles != null",
  "!methodHandles.keySet().contains(null)",
  "Iterables.all(methodHandles.values(), " +
      "Predicates.<MethodContractHandle>all(Predicates.nonNull()))"
})
class ContractAnalyzer extends ClassVisitor {
  protected List<ClassContractHandle> classHandles;
  protected Map<String, ArrayList<MethodContractHandle>> methodHandles;

  protected String className;
  protected MethodNode lastMethodNode;

  /**
   * Constructs an empty ContractAnalyzer. The ContractAnalyzer is
   * intended to be filled through its visitor interface.
   */
  ContractAnalyzer() {
    super(Opcodes.ASM5);
    classHandles = new ArrayList<ClassContractHandle>();
    methodHandles = new HashMap<String, ArrayList<MethodContractHandle>>();
  }

  /**
   * Returns the ClassHandle objects matching the specified criteria.
   *
   * @param kind the kind of the handles
   * @return a list containing the requested handles
   */
  @Requires("kind != null")
  @Ensures({
    "result != null",
    "!result.contains(null)"
  })
  List<ClassContractHandle> getClassHandles(ContractKind kind) {
    ArrayList<ClassContractHandle> matched =
        new ArrayList<ClassContractHandle>();
    for (ClassContractHandle h : classHandles) {
      if (kind.equals(h.getKind())) {
        matched.add(h);
      }
    }
    return matched;
  }

  /**
   * Returns the MethodHandle objects matching the specified criteria.
   *
   * @param kind the kind of the handles
   * @param name the target method name
   * @param desc the target method descriptor
   * @param extraCount the number of extra parameters in the contract
   * method
   * @return a list containing the requested handles
   */
  @Requires({
    "kind != null",
    "name != null",
    "desc != null",
    "extraCount >= 0"
  })
  @Ensures({
    "result != null",
    "!result.contains(null)"
  })
  List<MethodContractHandle> getMethodHandles(ContractKind kind,
      String name, String desc, int extraCount) {
    ArrayList<MethodContractHandle> candidates = methodHandles.get(name);
    if (candidates == null) {
      return Collections.emptyList();
    }

    ArrayList<MethodContractHandle> matched =
        new ArrayList<MethodContractHandle>();
    for (MethodContractHandle h : candidates) {
      if (kind.equals(h.getKind())
          && descArgumentsMatch(desc, h.getContractMethod().desc, extraCount)) {
        matched.add(h);
      }
    }
    return matched;
  }

  /**
   * Returns the first ClassHandle object matching the specified
   * criteria.
   *
   * @param kind the kind of the handle
   * @return a handle matching the criteria, or {@code null}
   */
  @Requires("kind != null")
  ClassContractHandle getClassHandle(ContractKind kind) {
    ArrayList<ClassContractHandle> matched =
        new ArrayList<ClassContractHandle>();
    for (ClassContractHandle h : classHandles) {
      if (kind.equals(h.getKind())) {
        return h;
      }
    }
    return null;
  }

  /**
   * Returns the first MethodHandle object matching the specified
   * criteria.
   *
   * @param kind the kind of the handle
   * @param name the target method name
   * @param desc the target method descriptor
   * @param extraCount the number of extra parameters in the contract
   * method
   * @return a handle matching the criteria, or {@code null}
   */
  @Requires({
    "kind != null",
    "name != null",
    "desc != null",
    "extraCount >= 0"
  })
  MethodContractHandle getMethodHandle(ContractKind kind,
      String name, String desc, int extraCount) {
    ArrayList<MethodContractHandle> candidates = methodHandles.get(name);
    if (candidates == null) {
      return null;
    }

    for (MethodContractHandle h : candidates) {
      if (kind.equals(h.getKind())
          && descArgumentsMatch(desc, h.getContractMethod().desc, extraCount)) {
        return h;
      }
    }
    return null;
  }

  /**
   * Returns {@code true} if the argument descriptors {@code desc1}
   * and {@code desc2} are equal (return type is ignored), ignoring
   * the last {@code offset} parameters of {@code desc2}.
   */
  @Requires({
    "desc1 != null",
    "desc2 != null",
    "offset >= 0"
  })
  private boolean descArgumentsMatch(String desc1, String desc2, int offset) {
    Type[] types1 = Type.getArgumentTypes(desc1);
    Type[] types2 = Type.getArgumentTypes(desc2);

    if (types2.length - types1.length != offset) {
      return false;
    }
    for (int i = 0; i < types1.length; ++i) {
      if (!types1[i].equals(types2[i])) {
        return false;
      }
    }
    return true;
  }

  /*
   * Visitor implementation.
   */

  @Override
  public void visit(int version, int access,
                    String name, String signature,
                    String superName, String[] interfaceNames) {
    className = name;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
                                   String signature, String[] exceptions) {
    captureLastMethodNode();
    lastMethodNode = new MethodNode(access, name, desc, signature, exceptions);
    return lastMethodNode;
  }

  @Override
  public void visitEnd() {
    captureLastMethodNode();
  }

  /**
   * Creates a contract handle for the method last visited, if it was
   * a contract method.
   */
  @Ensures("lastMethodNode == null")
  protected void captureLastMethodNode() {
    if (lastMethodNode == null) {
      return;
    }

    ContractKind kind = ContractMethodSignatures.getKind(lastMethodNode);
    if (kind != null) {
      List<Long> lineNumbers =
          ContractMethodSignatures.getLineNumbers(lastMethodNode);

      if (kind.isClassContract() || kind.isHelperContract()) {
        ClassContractHandle ch =
            new ClassContractHandle(kind, className,
                                    lastMethodNode, lineNumbers);
        classHandles.add(ch);
      } else {
        MethodContractHandle mh =
            new MethodContractHandle(kind, className,
                                     lastMethodNode, lineNumbers);
        internMethod(mh.getMethodName()).add(mh);
      }
    }

    lastMethodNode = null;
  }

  /**
   * Returns the contract handle collection corresponding to the
   * method named {@code name}.
   */
  @Requires("name != null")
  @Ensures({
    "result != null",
    "result == methodHandles.get(name)"
  })
  protected List<MethodContractHandle> internMethod(String name) {
    ArrayList<MethodContractHandle> handles = methodHandles.get(name);
    if (handles == null) {
      handles = new ArrayList<MethodContractHandle>();
      methodHandles.put(name, handles);
    }
    return handles;
  }
}
