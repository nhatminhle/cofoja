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

import com.google.java.contract.ContractEnvironment;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.util.DebugUtils;
import com.google.java.contract.core.util.JavaUtils;
import org.objectweb.asm.ClassReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;
import javax.tools.JavaFileObject.Kind;

/**
 * A Java agent premain class that sets up class instrumentation for
 * contracts or can be run as a standalone program that instruments
 * class files.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
public class PreMain {
  @Invariant("transformer != null")
  private static class DumpClassFileTransformer
      implements ClassFileTransformer {
    protected ClassFileTransformer transformer;

    @Requires({
      "parent != null",
      "dumpDir != null"
    })
    public DumpClassFileTransformer(ClassFileTransformer parent,
                                    String dumpDir) {
      transformer = parent;
      DebugUtils.setDumpDirectory(dumpDir);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> redefinedClass,
                            ProtectionDomain protectionDomain,
                            byte[] bytecode)
        throws IllegalClassFormatException {
      byte[] data = transformer.transform(loader, className, redefinedClass,
                                          protectionDomain, bytecode);
      if (data != null) {
        DebugUtils.dump(className, data, Kind.CLASS);
      }
      return data;
    }
  }

  private static void configure() {
    String configClass = System.getProperty("com.google.java.contract.configurator");
    if (configClass != null) {
      try {
        Class<?> clazz = Class.forName(configClass);
        Object instance = clazz.newInstance();
        clazz.getMethod("configure", ContractEnvironment.class)
            .invoke(instance, new AgentContractEnvironment());
      } catch (ClassNotFoundException e) {
        DebugUtils.warn("agent", "cannot find configurator class");
      } catch (NoSuchMethodException e) {
        DebugUtils.warn("agent", "cannot find configure method");
      } catch (InvocationTargetException e) {
        DebugUtils.warn("agent", "configure method threw an exception: "
                        + e.getTargetException().toString());
      } catch (Exception e) {
        DebugUtils.warn("agent",
                        "error during configure method: " + e.toString());
      }
    }
  }

  public static void premain(String args, Instrumentation inst) {
    ClassFileTransformer transformer = new ContractClassFileTransformer();

    String dumpDir = System.getProperty("com.google.java.contract.dump");
    if (dumpDir != null) {
      transformer = new DumpClassFileTransformer(transformer, dumpDir);
    }

    inst.addTransformer(transformer);

    configure();
  }

  public static void main(String[] args)
      throws IllegalClassFormatException, IOException {
    String classout =
      System.getProperty("com.google.java.contract.classoutput");
    /* TODO(lenh): Separate class loader for source files. */
    instrument(args, classout, null);
  }

  public static void instrument(String[] args, String classout,
                                ClassLoader loader)
      throws IllegalClassFormatException, IOException {
    ContractClassFileTransformer transformer;
    if (loader == null) {
      transformer = new ContractClassFileTransformer();
    } else {
      transformer = new ContractClassFileTransformer(loader);
    }
    configure();

    for (String arg : args) {
      String baseName = arg;
      if (arg.endsWith(Kind.CLASS.extension)) {
        baseName = baseName
            .substring(0, baseName.length() - Kind.CLASS.extension.length());
      }

      /*
       * Ignore helper class files, which are handled along with their
       * interface.
       */
      if (baseName.endsWith(JavaUtils.HELPER_CLASS_SUFFIX)) {
        continue;
      }

      /*
       * Compute file names for all class files potentially
       * involved.
       */
      File fileName = new File(baseName + Kind.CLASS.extension);
      File contractFileName =
          new File(baseName + JavaUtils.CONTRACTS_EXTENSION);
      File helperFileName =
          new File(baseName + JavaUtils.HELPER_CLASS_SUFFIX
                   + Kind.CLASS.extension);

      byte[] bytecode = getBytes(fileName);
      byte[] instrumented = null;
      byte[] instrumentedHelper = null;

      File outputFileName;
      File helperOutputFileName;
      if (classout == null) {
        outputFileName = new File(baseName + JavaUtils.CONTRACTED_EXTENSION);
        helperOutputFileName =
            new File(baseName + JavaUtils.HELPER_CLASS_SUFFIX
                     + JavaUtils.CONTRACTED_EXTENSION);
      } else {
        String className = getClassName(bytecode);
        String baseOutputName = classout + "/" + className;
        outputFileName = new File(baseOutputName + Kind.CLASS.extension);
        helperOutputFileName =
            new File(baseOutputName + JavaUtils.HELPER_CLASS_SUFFIX
                     + Kind.CLASS.extension);
      }

      /*
       * - If argument is an interface, instrument helper, copy interface.
       * - If argument is a contracted class, instrument class.
       * - Otherwise, copy class file.
       */
      if (helperFileName.isFile()) {
        byte[] helperBytecode = getBytes(helperFileName);
        instrumentedHelper = transformer.transformWithDebug(helperBytecode);
      } else if (contractFileName.isFile()) {
        byte[] contractBytecode = getBytes(contractFileName);
        instrumented =
            transformer.transformWithContracts(bytecode, contractBytecode);
      }

      outputFileName.getParentFile().mkdirs();
      FileOutputStream out = new FileOutputStream(outputFileName);
      out.write(instrumented == null ? bytecode : instrumented);
      out.close();

      if (instrumentedHelper != null) {
        helperOutputFileName.getParentFile().mkdirs();
        FileOutputStream helperOut = new FileOutputStream(helperOutputFileName);
        helperOut.write(instrumentedHelper);
        helperOut.close();
      }
    }
  }

  private static byte[] getBytes(File path) throws IOException {
    FileInputStream in = new FileInputStream(path);
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    byte[] buffer = new byte[1024];
    int len;
    while ((len = in.read(buffer)) != -1) {
      out.write(buffer, 0, len);
    }
    in.close();

    return out.toByteArray();
  }

  private static String getClassName(byte[] bytecode)
      throws IllegalClassFormatException {
    try {
      return new ClassReader(bytecode).getClassName();
    } catch (Throwable t) {
      /* If the class file contains errors, ASM will just crash. */
      IllegalClassFormatException e = new IllegalClassFormatException();
      e.initCause(t);
      throw e;
    }
  }
}
