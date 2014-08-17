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
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ClassName;
import com.google.java.contract.core.runtime.BlacklistManager;
import com.google.java.contract.core.util.DebugUtils;
import com.google.java.contract.core.util.JavaUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
@ContractImport("com.google.java.contract.core.model.ClassName")
public class ContractClassFileTransformer implements ClassFileTransformer {
  /**
   * Find and store superclass information.
   */
  private class SuperInfoFinder extends ClassVisitor {
    private SuperInfoFinder() {
      super(Opcodes.ASM5);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
      super.visit(version, access, name, signature, superName, interfaces);

      HashSet<String> assignable = new HashSet<String>();
      assignable.add(name);
      assignableToNames.put(name, assignable);

      if (superName != null) {
        superClassNames.put(name, superName);
        assignable.add(superName);
        findSuperInfo(superName);
        assignable.addAll(assignableToNames.get(superName));
      }

      for (String ifaceName : interfaces) {
        assignable.add(ifaceName);
        findSuperInfo(ifaceName);
        assignable.addAll(assignableToNames.get(ifaceName));
      }
    }

    /**
     * Look up information about super classes and assignable types
     * for the class named {@code className}.
     */
    @Requires("ClassName.isBinaryName(className)")
    private void findSuperInfo(String className) {
      if (superClassNames.containsKey(className)) {
        return;
      }
      if (blacklistManager.isIgnored(new ClassName(className)
                                     .getQualifiedName())) {
        findSuperInfoFromClass(className);
      } else {
        findSuperInfoFromClassFile(className);
      }
    }

    @Requires("ClassName.isBinaryName(className)")
    private void findSuperInfoFromClassFile(String className) {
      try {
        InputStream stream = JavaUtils.getClassInputStream(loader, className);
        if (stream == null)
          throw new NullPointerException();
        ClassReader reader = new ClassReader(stream);
        reader.accept(this, 0);
      } catch (Exception e) {
        addDefaultAssignable(className);
      }
    }

    @Requires("ClassName.isBinaryName(className)")
    private void findSuperInfoFromClass(String className) {
      Class<?> clazz;
      try {
        String qName = new ClassName(className).getQualifiedName();
        clazz = Class.forName(qName, false, loader);
      } catch (ClassNotFoundException e) {
        addDefaultAssignable(className);
        return;
      }

      Class<?> superClass = clazz.getSuperclass();
      if (superClass == null) {
        addDefaultAssignable(className);
        return;
      }

      HashSet<String> assignable = new HashSet<String>();
      assignable.add(className);
      assignableToNames.put(className, assignable);

      String superName = superClass.getName().replace('.', '/');
      superClassNames.put(className, superName);
      assignable.add(superName);
      findSuperInfo(superName);
      assignable.addAll(assignableToNames.get(superName));

      for (Class<?> iface : clazz.getInterfaces()) {
        String ifaceName = iface.getName().replace('.', '/');
        assignable.add(ifaceName);
        findSuperInfo(ifaceName);
        assignable.addAll(assignableToNames.get(ifaceName));
      }
    }

    /**
     * Add default super type information for the class named
     * {@code className}. The default information makes the class
     * a direct child of Object and assignable to it (and to itself)
     * as well.
     */
    @Requires("className != null")
    private void addDefaultAssignable(String className) {
      if (!superClassNames.containsKey(className)) {
        superClassNames.put(className, "java/lang/Object");
      }
      HashSet<String> assignable = new HashSet<String>();
      assignable.add(className);
      assignable.add("java/lang/Object");
      assignableToNames.put(className, assignable);
    }
  }

  /**
   * A ClassWriter that does not load new classes. Tries to get the
   * information from class files; an exception is made for
   * blacklisted classes, which <em>are</em> loaded as usual. There
   * should be no conflict as blacklisted hierarchies should be
   * distinct from contracted ones.
   */
  protected class NonLoadingClassWriter extends ClassWriter {
    @Requires("reader != null")
    public NonLoadingClassWriter(ClassReader reader, int flags) {
      super(reader, flags);
    }

    /*
     * TODO(lenh): IMPORTANT NOTE. Computing stack frames in a purely
     * forward fashion (from definitions to uses) using this method
     * cannot be correct in some cases, no matter how accurate the
     * type we return here. If two supertypes are possible (e.g., two
     * interfaces) and one of them is needed for a following call,
     * there's a 1/2 chance to pick the wrong one since the use is
     * unknown at the time of the merge definition. It is unclear to
     * me what the expected semantics of this method are, or what
     * analysis (forward or backward) is actually performed by ASM
     * when computing frames. Also, as is the case with the official
     * implementation, this method does not handle interfaces
     * completely.
     */
    @Override
    protected String getCommonSuperClass(String className1, String className2) {
      if (className1.equals(className2)) {
        return className1;
      }
      SuperInfoFinder superInfoFinder = new SuperInfoFinder();
      superInfoFinder.findSuperInfo(className1);
      superInfoFinder.findSuperInfo(className2);
      if (assignableToNames.get(className1).contains(className2)) {
        return className2;
      }
      while (!assignableToNames.get(className2).contains(className1)) {
        className1 = superClassNames.get(className1);
      }
      return className1;
    }
  }

  protected BlacklistManager blacklistManager;

  protected ClassLoader loader;

  /*
   * TODO(lenh): Use a LinkedHashMap for the two following fields,
   * with some caching mechanism.
   */

  protected Map<String, Set<String>> assignableToNames =
      new HashMap<String, Set<String>>();

  protected Map<String, String> superClassNames = new HashMap<String, String>();

  /**
   * Constructs a new ContractClassFileTransformer.
   */
  public ContractClassFileTransformer() {
    blacklistManager = BlacklistManager.getInstance();
  }

  /**
   * Constructs a new ContractClassFileTransformer with default class
   * loader {@code loader}. Subsequently,
   * {@link #instrumentWithContracts(byte[],ContractAnalyzer)} may be
   * called directly and will use the default loader.
   */
  public ContractClassFileTransformer(ClassLoader loader) {
    this();
    this.loader = loader;
  }

  /**
   * Instruments the specified class, if necessary.
   */
  @Override
  public byte[] transform(ClassLoader loader, String className,
      Class<?> redefinedClass, ProtectionDomain protectionDomain,
      byte[] bytecode) {
    if (blacklistManager.isIgnored(className)) {
      DebugUtils.info("agent", "ignoring " + className);
      return null;
    }
    try {
      this.loader = loader;
      ContractAnalyzer contracts = analyze(className);
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
   * @param className the class name
   * @return the extracted contracts or {@code null} if the class has
   * none and should not be instrumented
   */
  @Requires("ClassName.isBinaryName(className)")
  protected ContractAnalyzer analyze(String className)
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
