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

import com.google.java.contract.AllowUnusedImport;
import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ClassName;
import com.google.java.contract.core.runtime.BlacklistManager;
import com.google.java.contract.core.runtime.ContractedChecker;
import com.google.java.contract.core.util.DebugUtils;
import com.google.java.contract.core.util.JavaUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import javax.tools.JavaFileObject.Kind;

/**
 * A class transformer responsible for instrumenting classes with
 * contracts. Only classes that have contracts will be instrumented.
 *
 * <p>The transformation process works in two steps:
 *
 * <ol>
 * <li>The contract methods are extracted recursively from the
 * contract class files and stored in the {@link ContractCodePool}.
 * <li>Each individual class is instrumented with its contracts, taken
 * from the pool.
 * </ol>
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
@AllowUnusedImport(ClassName.class)
@Invariant("contractedChecker != null")
public class ContractClassFileTransformer implements ClassFileTransformer {
  /**
   * A ClassWriter that does not load any class.
   */
  protected class NonLoadingClassWriter extends ClassWriter {
    @Requires("reader != null")
    public NonLoadingClassWriter(ClassReader reader, int flags) {
      super(reader, flags);
    }

    /*
     * TODO(lenh): The contract of this method only specifies that it
     * should return a common supertype; it does not say it has to be
     * the best possible, and just returning java.lang.Object seems to
     * work fine, but probably defeats all matter of run time type
     * verification through frame check instructions.
     *
     * The relative usefulness of a switch to a more complete
     * non-class-loading implementation as provided by the ASM package
     * examples should be assessed.
     */
    protected String getCommonSuperClass(String type1, String type2) {
      return "java/lang/Object";
    }
  }

  protected BlacklistManager blacklistManager;
  protected ContractedChecker contractedChecker;

  /**
   * Constructs a new ContractClassFileTransformer.
   */
  public ContractClassFileTransformer() {
    blacklistManager = BlacklistManager.getInstance();
    contractedChecker = ContractedChecker.getInstance();
  }

  /**
   * Instruments the specified class, if necessary.
   */
  @Override
  public byte[] transform(ClassLoader loader, String className,
      Class<?> redefinedClass, ProtectionDomain protectionDomain,
      byte[] bytecode)
      throws IllegalClassFormatException {
    if (blacklistManager.isIgnored(className)) {
      DebugUtils.info("agent", "ignoring " + className);
      return null;
    }
    try {
      ContractAnalyzer contracts = analyze(loader, className);
      if (contracts == null) {
        if (className.endsWith(JavaUtils.HELPER_CLASS_SUFFIX)) {
          DebugUtils.info("agent", "adding source info to " + className);
          return instrumentWithDebug(bytecode);
        } else {
          return null;
        }
      } else {
        DebugUtils.info("agent", "adding contracts to " + className);
        return instrumentWithContracts(bytecode, contracts);
      }
    } catch (Throwable e) {
      DebugUtils.err("agent", "while instrumenting " + className, e);
      /* Not reached. */
      throw new RuntimeException(e);
    }
  }

  /**
   * Instruments the specified class with contracts.
   */
  @Requires({
    "bytecode != null",
    "contractBytecode != null"
  })
  @Ensures("result != null")
  public byte[] transformWithContracts(byte[] bytecode, byte[] contractBytecode)
      throws IllegalClassFormatException {
    try {
      ContractAnalyzer contracts =
          extractContracts(new ClassReader(contractBytecode));
      return instrumentWithContracts(bytecode, contracts);
    } catch (Throwable t) {
      /* If the class file contains errors, ASM will just crash. */
      IllegalClassFormatException e = new IllegalClassFormatException();
      e.initCause(t);
      throw e;
    }
  }

  /**
   * Instruments the specified class with debug information.
   */
  @Requires("bytecode != null")
  @Ensures("result != null")
  public byte[] transformWithDebug(byte[] bytecode)
      throws IllegalClassFormatException {
    try {
      return instrumentWithDebug(bytecode);
    } catch (Throwable t) {
      /* If the class file contains errors, ASM will just crash. */
      IllegalClassFormatException e = new IllegalClassFormatException();
      e.initCause(t);
      throw e;
    }
  }

  /**
   * Extracts contract methods for the specified class, if necessary.
   *
   * @param loader the class loader used to load resources
   * @param className the class name
   * @return the extracted contracts or {@code null} if the class has
   * none and should not be instrumented
   */
  @Requires("ClassName.isBinaryName(className)")
  protected ContractAnalyzer analyze(ClassLoader loader, String className)
      throws IOException {
    /* Skip helper classes. */
    if (className.endsWith(JavaUtils.HELPER_CLASS_SUFFIX)) {
      return null;
    }

    /* Skip interfaces. */
    String helperFileName = className + JavaUtils.HELPER_CLASS_SUFFIX
        + Kind.CLASS.extension;
    if (JavaUtils.resourceExists(loader, helperFileName)) {
      return null;
    }

    /* Try to get contracts class file. */
    InputStream contractStream =
        JavaUtils.getContractClassInputStream(loader, className);
    if (contractStream == null) {
      contractedChecker.addFutureContractedCheck(className.replace('/', '.'));
      return null;
    }

    return extractContracts(new ClassReader(contractStream));
  }

  /**
   * Processes the specified reader and returns extracted contracts.
   */
  @Requires("reader != null")
  protected ContractAnalyzer extractContracts(ClassReader reader) {
    ContractAnalyzer contractAnalyzer = new ContractAnalyzer();
    reader.accept(contractAnalyzer, ClassReader.EXPAND_FRAMES);
    return contractAnalyzer;
  }

  /**
   * Instruments the passed class file so that it contains contract
   * methods and calls to these methods. The contract information is
   * retrieved from the {@link ContractCodePool}.
   *
   * @param bytecode the bytecode of the class
   * @param contracts the extracted contracts for the class
   * @return the instrumented bytecode of the class
   */
  @Requires({
    "bytecode != null",
    "contracts != null"
  })
  @Ensures("result != null")
  protected byte[] instrumentWithContracts(byte[] bytecode,
                                           ContractAnalyzer contracts) {
    ClassReader reader = new ClassReader(bytecode);
    ClassWriter writer =
        new NonLoadingClassWriter(reader,
                                  ClassWriter.COMPUTE_FRAMES |
                                  ClassWriter.COMPUTE_MAXS);

    SpecificationClassAdapter adapter =
        new SpecificationClassAdapter(writer, contracts);
    reader.accept(adapter, ClassReader.EXPAND_FRAMES);

    return writer.toByteArray();
  }

  /**
   * Instruments the passed class file so that it contains debug
   * information extraction from annotations.
   */
  @Requires("bytecode != null")
  @Ensures("result != null")
  private byte[] instrumentWithDebug(byte[] bytecode) {
    ClassReader reader = new ClassReader(bytecode);
    ClassWriter writer = new NonLoadingClassWriter(reader, 0);
    reader.accept(new HelperClassAdapter(writer), ClassReader.EXPAND_FRAMES);
    return writer.toByteArray();
  }
}
