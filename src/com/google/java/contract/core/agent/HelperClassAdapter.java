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

import com.google.java.contract.Requires;
import com.google.java.contract.core.util.JavaUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.tools.JavaFileObject.Kind;

/**
 * Adds debug information to a helper class.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
class HelperClassAdapter extends ClassVisitor {
  protected class HelperMethodAdapter extends LineNumberingMethodAdapter {
    /**
     * Constructs a new HelperMethodAdapter.
     *
     * @param mv the MethodVisitor this adapter delegates to
     * @param access the access flags of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @Requires({
      "mv != null",
      "name != null",
      "desc != null"
    })
    public HelperMethodAdapter(MethodVisitor mv, int access,
                               String name, String desc) {
      super(mv, access, name, desc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      if (Type.getType(desc).getInternalName().equals(
              "com/google/java/contract/core/agent/ContractMethodSignature")) {
        return new AnnotationVisitor(Opcodes.ASM5) {
          @Override
          public void visit(String name, Object value) {
            if (name.equals("lines")) {
              lineNumbers = ContractMethodSignatures.getLineNumbers(value);
            }
          }
        };
      }
      return super.visitAnnotation(desc, visible);
    }
  }

  @Requires("cv != null")
  public HelperClassAdapter(ClassVisitor cv) {
    super(Opcodes.ASM5, cv);
  }

  @Override
  public void visitSource(String source, String debug) {
    /*
     * Work around bogus file names and remove helper suffix if
     * found. The compiler, as invoked in ContractJavaCompiler,
     * produces SourceFile entries with a trailing ']' for no apparent
     * reason; this method removes any trailing characters after the
     * source extension.
     */
    String name =
        source.substring(0, source.lastIndexOf(Kind.SOURCE.extension));
    if (name.endsWith(JavaUtils.HELPER_CLASS_SUFFIX)) {
      int lengthSansSuffix =
          name.length() - JavaUtils.HELPER_CLASS_SUFFIX.length();
      name = name.substring(0, lengthSansSuffix);
    }
    name += Kind.SOURCE.extension;
    cv.visitSource(name, debug);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
                                   String signature, String[] exceptions) {
    MethodVisitor mv =
        cv.visitMethod(access, name, desc, signature, exceptions);
    return new HelperMethodAdapter(mv, access, name, desc);
  }
}
