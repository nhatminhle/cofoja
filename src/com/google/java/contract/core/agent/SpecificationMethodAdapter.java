/*
 * Copyright 2007 Johannes Rieken
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
package com.google.java.contract.core.agent;

import com.google.java.contract.ContractImport;
import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ContractKind;
import com.google.java.contract.core.util.DebugUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A bytecode method visitor that instruments the original method to
 * add calls to contract methods, and injects these contract methods,
 * if necessary, into the enclosing class.
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
  "methodStart != null",
  "methodEnd != null",
  "contracts != null",
  "ClassName.isBinaryName(className)",
  "methodName != null",
  "methodDesc != null",
  "Iterables.all(oldValueLocals, Predicates.between(0, null))",
  "Iterables.all(signalOldValueLocals, Predicates.between(0, null))"
})
public class SpecificationMethodAdapter extends AdviceAdapter {
  /*
   * Reflection constants used in instrumentation code.
   */
  private static final Type CLASS_TYPE =
      Type.getObjectType("java/lang/Class");
  private static final Type EXCEPTION_TYPE =
      Type.getObjectType("java/lang/Exception");
  private static final Type CONTRACT_RUNTIME_TYPE =
      Type.getObjectType("com/google/java/contract/core/runtime/ContractRuntime");
  private static final Type CONTRACT_CONTEXT_TYPE =
      Type.getObjectType("com/google/java/contract/core/runtime/ContractContext");
  private static final Method GET_CLASS_METHOD =
      Method.getMethod("java.lang.Class getClass()");
  private static final Method GET_CONTEXT_METHOD =
      Method.getMethod("com.google.java.contract.core.runtime.ContractContext "
                       + "getContext()");
  private static final Method TRY_ENTER_CONTRACT_METHOD =
      Method.getMethod("boolean tryEnterContract()");
  private static final Method LEAVE_CONTRACT_METHOD =
      Method.getMethod("void leaveContract()");
  private static final Method TRY_ENTER_METHOD =
      Method.getMethod("boolean tryEnter(Object)");
  private static final Method LEAVE_METHOD =
      Method.getMethod("void leave(Object)");

  /*
   * Used to bracket the entire original method to catch any exception
   * that may arise and relay it to the exceptional postconditions.
   */
  protected Label methodStart;
  protected Label methodEnd;

  protected ContractAnalyzer contracts;
  protected String className;
  protected String methodName;
  protected String methodDesc;
  protected Type thisType;

  protected boolean statik;
  protected boolean isConstructor;
  protected boolean isStaticInit;

  protected int contextLocal;
  protected int checkInvariantsLocal;
  protected List<Integer> oldValueLocals;
  protected List<Integer> signalOldValueLocals;

  protected SpecificationClassAdapter classAdapter;

  protected boolean withPreconditions;
  protected boolean withPostconditions;
  protected boolean withInvariants;

  /**
   * Constructs a new SpecificationClassAdapter.
   *
   * @param ca the class adapter which has spawned this method adapter
   * @param mv the method visitor to delegate to
   * @param access the method access bit mask
   * @param methodName the name of the method
   * @param methodDesc the descriptor of the method
   */
  @Requires({
    "ca != null",
    "mv != null",
    "methodName != null",
    "methodDesc != null"
  })
  public SpecificationMethodAdapter(SpecificationClassAdapter ca,
                                    MethodVisitor mv,
                                    int access, String methodName,
                                    String methodDesc) {
    super(Opcodes.ASM5, mv, access, methodName, methodDesc);

    methodStart = new Label();
    methodEnd = new Label();

    this.contracts = ca.getContracts();
    className = ca.getClassName();
    this.methodName = methodName;
    this.methodDesc = methodDesc;
    thisType = Type.getType("L" + className + ";");

    statik = (access & ACC_STATIC) != 0;
    isConstructor = methodName.equals("<init>");
    isStaticInit = methodName.endsWith("<clinit>");

    contextLocal = -1;
    checkInvariantsLocal = -1;
    oldValueLocals = new ArrayList<Integer>();
    signalOldValueLocals = new ArrayList<Integer>();

    classAdapter = ca;

    ActivationRuleManager am = ActivationRuleManager.getInstance();
    withPreconditions = am.hasPreconditionsEnabled(className);
    withPostconditions = am.hasPostconditionsEnabled(className);
    withInvariants = am.hasInvariantsEnabled(className);
  }

  /**
   * Returns {@code true} if the specified label is resolved, that is,
   * is associated with an offset in the bytecode of the method.
   */
  @Requires("label != null")
  protected static boolean labelIsResolved(Label label) {
    try {
      label.getOffset();
    } catch (IllegalStateException e) {
      return false;
    }
    return true;
  }

  @Override
  public void visitLocalVariable(String name, String desc, String signature,
                                 Label start, Label end, int index) {
    /*
     * Handle bogus debug information where labels are not on
     * instruction boundaries. EMMA, the coverage instrumenter in use
     * at Google, produces such code.
     */
    if (!labelIsResolved(start)) {
      return;
    }
    if (!labelIsResolved(end)) {
      end = start;
    }
    super.visitLocalVariable(name, desc, signature, start, end, index);
  }

  /**
   * Advises the method by injecting invariants, precondition
   * assertions, and old value computations, before the original code.
   */
  @Override
  protected void onMethodEnter() {
    if (withPreconditions || withPostconditions || withInvariants) {
      enterContractedMethod();

      if (withPostconditions) {
        allocateOldValues(ContractKind.OLD, oldValueLocals);
        allocateOldValues(ContractKind.SIGNAL_OLD, signalOldValueLocals);
      }

      mark(methodStart);

      Label skip = enterBusySection();

      if (withInvariants && !statik && !isConstructor && !isStaticInit) {
        invokeInvariants();
      }

      if (withPreconditions) {
        invokePreconditions();
      }

      if (withPostconditions) {
        invokeOldValues(ContractKind.OLD, oldValueLocals);
        invokeOldValues(ContractKind.SIGNAL_OLD, signalOldValueLocals);
      }

      leaveBusySection(skip);
    }
  }

  /**
   * Advises the method by injecting postconditions and invariants
   * on exit from the original code ({@code return} or {@throw}).
   */
  @Override
  protected void onMethodExit(int opcode) {
    if ((withPreconditions || withPostconditions || withInvariants)
        && opcode != ATHROW) {
      if (withPostconditions || withInvariants) {
        Label skip = enterBusySection();

        if (withPostconditions) {
          Type returnType = Type.getReturnType(methodDesc);
          int returnIndex = -1;
          if (returnType.getSort() != Type.VOID) {
            if (returnType.getSize() == 2) {
              dup2();
            } else {
              dup();
            }
            returnIndex = newLocal(returnType);
            storeLocal(returnIndex);
          }
          invokeCommonPostconditions(ContractKind.POST, oldValueLocals,
                                     returnIndex);
        }

        if (withInvariants && !statik) {
          invokeInvariants();
        }

        leaveBusySection(skip);
      }
      leaveContractedMethod();
    }
  }

  /**
   * Advises the method by injecting exceptional postconditions and
   * invariants after the original code. This code only gets executed
   * if an exception has been thrown (otherwise, a {@code return}
   * instruction would have ended execution of the method already).
   */
  @Override
  public void visitMaxs(int maxStack, int maxLocals) {
    if (withPreconditions || withPostconditions || withInvariants) {
      mark(methodEnd);
      catchException(methodStart, methodEnd, null);

      if (withPostconditions) {
        Label skipEx = new Label();
        dup();
        instanceOf(EXCEPTION_TYPE);
        ifZCmp(EQ, skipEx);

        Label skip = enterBusySection();
        int throwIndex = newLocal(EXCEPTION_TYPE);
        checkCast(EXCEPTION_TYPE);
        storeLocal(throwIndex);

        invokeCommonPostconditions(ContractKind.SIGNAL, signalOldValueLocals,
                                   throwIndex);
        if (withInvariants && !statik) {
          invokeInvariants();
        }

        loadLocal(throwIndex);
        leaveBusySection(skip);

        mark(skipEx);
      }

      /*
       * The exception to throw is on the stack and
       * leaveContractedMethod() does not alter that fact.
       */
      leaveContractedMethod();
      throwException();
    }
    super.visitMaxs(maxStack, maxLocals);
  }

  /**
   * Injects code to allocate the local variables needed to hold old
   * values for the postconditions of the method. These variables are
   * initialized to {@code null}.
   *
   * @param kind either OLD or SIGNAL_OLD
   * @param list the list that will hold the allocated indexes
   */
  @Requires({
    "kind != null",
    "kind.isOld()",
    "list != null"
  })
  protected void allocateOldValues(ContractKind kind, List<Integer> list) {
    List<MethodContractHandle> olds =
        contracts.getMethodHandles(kind, methodName, methodDesc, 0);
    if (olds.isEmpty()) {
      return;
    }

    Integer[] locals = new Integer[olds.size()];
    for (MethodContractHandle h : olds) {
      int k = h.getKey();
      locals[k] = newLocal(Type.getReturnType(h.getContractMethod().desc));
      push((String) null);
      storeLocal(locals[k]);
    }
    list.addAll(Arrays.asList(locals));
  }

  /**
   * Injects calls to old value contract methods. old value contract
   * methods get called with, in this order:
   *
   * <ul>
   * <li>the {@code this} pointer, if any;
   * <li>and the original method's parameters.
   * </ul>
   *
   * @param kind either OLD or SIGNAL_OLD
   * @param list the list that holds the allocated old value variable
   * indexes
   */
  @Requires({
    "kind != null",
    "kind.isOld()",
    "list != null"
  })
  protected void invokeOldValues(ContractKind kind, List<Integer> list) {
    List<MethodContractHandle> olds =
        contracts.getMethodHandles(kind, methodName, methodDesc, 0);
    if (olds.isEmpty()) {
      return;
    }

    for (MethodContractHandle h : olds) {
      MethodNode contractMethod = injectContractMethod(h);
      int k = h.getKey();

      if (!statik) {
        loadThis();
      }
      loadArgs();
      invokeContractMethod(contractMethod);

      storeLocal(list.get(k));
    }
  }

  /**
   * Injects calls to invariant contract methods. Invariant contract
   * methods get called with the {@code this} pointer, if any.
   */
  protected void invokeInvariants() {
    ClassContractHandle h = contracts.getClassHandle(ContractKind.INVARIANT);
    if (h == null) {
      return;
    }

    MethodNode contractMethod = injectContractMethod(h);

    Label skipInvariants = new Label();
    if (isConstructor) {
      loadThis();
      invokeVirtual(thisType, GET_CLASS_METHOD);
      loadThisClass();
      ifCmp(CLASS_TYPE, NE, skipInvariants);
    } else {
      loadLocal(checkInvariantsLocal);
      ifZCmp(EQ, skipInvariants);
    }

    if (!statik) {
      loadThis();
    }
    invokeContractMethod(contractMethod);

    mark(skipInvariants);
  }

  /**
   * Injects calls to precondition contract methods. Precondition
   * contract methods get called with, in this order:
   *
   * <ul>
   * <li>the {@code this} pointer, if any;
   * <li>and the original method's parameters.
   * </ul>
   */
  protected void invokePreconditions() {
    MethodContractHandle h =
        contracts.getMethodHandle(ContractKind.PRE, methodName, methodDesc, 0);
    if (h == null) {
      return;
    }

    MethodNode contractMethod = injectContractMethod(h);
    if (!statik) {
      loadThis();
    }
    loadArgs();
    invokeContractMethod(contractMethod);
  }

  /**
   * Injects calls to postcondition (or exceptional postcondition)
   * contract methods. Postcondition contract methods get called with,
   * in this order:
   *
   * <ul>
   * <li>the {@code this} pointer, if any;
   * <li>the original method's parameters;
   * <li>the return value, if the original method is not void, or the
   * exception object;
   * <li>and old values if any.
   * </ul>
   *
   * @param kind the kind of postcondition contract to invoke
   * @param oldLocals a list of old value variables
   * @param extraIndex the index of the local variable that holds the
   * return value, or exception object, of the method, or -1 if none
   */
  @Requires({
    "kind != null",
    "kind.isPostcondition()",
    "oldLocals != null",
    "extraIndex >= -1"
  })
  protected void invokeCommonPostconditions(ContractKind kind,
      List<Integer> oldLocals, int extraIndex) {
    MethodContractHandle h =
        contracts.getMethodHandle(kind, methodName, methodDesc,
                                  getPostDescOffset(oldLocals, extraIndex));
    if (h == null) {
      return;
    }

    MethodNode contractMethod = injectContractMethod(h);

    if (!statik) {
      loadThis();
    }
    loadArgs();

    if (extraIndex != -1) {
      loadLocal(extraIndex);
    }

    for (Integer oldIndex : oldLocals) {
      loadLocal(oldIndex);
    }

    invokeContractMethod(contractMethod);
  }

  /**
   * Computes the method descriptor offset of a postcondition (or an
   * exceptional postcondition). The method descriptor offset is the
   * number of extra arguments the contract method has in addition to
   * those inherited from the original method.
   *
   * @param oldLocals the list of old value variables
   * @param extraIndex the local variable index of an extra parameter,
   * or -1 if none
   */
  @Requires({
    "oldLocals != null",
    "extraIndex >= -1"
  })
  @Ensures("result >= 0")
  protected int getPostDescOffset(List<Integer> oldLocals, int extraIndex) {
    int off = 0;
    if (extraIndex != -1) {
      ++off;
    }
    off += oldLocals.size();
    return off;
  }

  /**
   * Injects the specified contract method code into the current
   * class, and returns a new Method object representing the injected
   * method. This is done by bypassing the SpecificationClassAdapter
   * and talking directly to its parent (usually, a class writer).
   *
   * @param handle the handle from the code pool that holds code and
   * meta-information about the contract method
   * @return the method node to invoke
   */
  @Requires("handle != null")
  @Ensures("result != null")
  protected MethodNode injectContractMethod(ContractHandle handle) {
    MethodNode methodNode = handle.getContractMethod();

    if (!handle.isInjected()) {
      DebugUtils.info("instrument", "contract method "
                      + className + "." + methodNode.name
                      + methodNode.desc);
      ClassVisitor cv = classAdapter.getParent();
      List<Long> lineNumbers = handle.getLineNumbers();
      if (lineNumbers != null) {
        cv = new LineNumberingClassAdapter(cv, lineNumbers);
      }
      methodNode.accept(new ContractFixingClassAdapter(cv));
      handle.setInjected(true);
    }

    return methodNode;
  }

  @Requires("contractMethod != null")
  protected void invokeContractMethod(MethodNode contractMethod) {
    if (!statik) {
      mv.visitMethodInsn(INVOKESPECIAL, className,
                         contractMethod.name, contractMethod.desc, false);
    } else {
      mv.visitMethodInsn(INVOKESTATIC, className,
                         contractMethod.name, contractMethod.desc, false);
    }
  }

  /**
   * Marks the beginning of a busy section. A busy section is skipped
   * if the context is already busy.
   */
  @Requires({
    "contextLocal >= 0",
    "checkInvariantsLocal >= 0"
  })
  @Ensures("result != null")
  protected Label enterBusySection() {
    Label skip = new Label();
    loadLocal(contextLocal);
    invokeVirtual(CONTRACT_CONTEXT_TYPE, TRY_ENTER_CONTRACT_METHOD);
    ifZCmp(EQ, skip);
    return skip;
  }

  /**
   * Marks the end of a busy section.
   */
  @Requires({
    "contextLocal >= 0",
    "skip != null"
  })
  protected void leaveBusySection(Label skip) {
    loadLocal(contextLocal);
    invokeVirtual(CONTRACT_CONTEXT_TYPE, LEAVE_CONTRACT_METHOD);
    mark(skip);
  }

  /**
   * Loads the static class object this method belongs to on the
   * stack.
   */
  protected void loadThisClass() {
    visitLdcInsn(thisType);
  }

  /**
   * Retrieves busy state of the current object.
   */
  @Ensures({
    "contextLocal >= 0",
    "checkInvariantsLocal >= 0"
  })
  protected void enterContractedMethod() {
    contextLocal = newLocal(CONTRACT_CONTEXT_TYPE);
    checkInvariantsLocal = newLocal(Type.BOOLEAN_TYPE);
    invokeStatic(CONTRACT_RUNTIME_TYPE, GET_CONTEXT_METHOD);
    dup();
    storeLocal(contextLocal);
    if (statik) {
      loadThisClass();
    } else {
      loadThis();
    }
    invokeVirtual(CONTRACT_CONTEXT_TYPE, TRY_ENTER_METHOD);
    storeLocal(checkInvariantsLocal);
  }

  /**
   * Cancels busy state of the current object.
   */
  @Requires("contextLocal >= 0")
  protected void leaveContractedMethod() {
    Label skip = new Label();
    loadLocal(checkInvariantsLocal);
    ifZCmp(EQ, skip);

    loadLocal(contextLocal);
    if (statik) {
      loadThisClass();
    } else {
      loadThis();
    }
    invokeVirtual(CONTRACT_CONTEXT_TYPE, LEAVE_METHOD);

    mark(skip);
  }
}
