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

import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * A class adapter that adds a line number to methods.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant("ContractMethodSignatures.isLineNumberList(lineNumbers)")
class LineNumberingClassAdapter extends ClassVisitor {
  protected List<Long> lineNumbers;

  /**
   * Constructs a new LineNumberingClassAdapter.
   *
   * @param cv the ClassVisitor this adapter delegates to
   * @param lineNumbers the line numbers to associate with methods
   */
  @Requires({
    "cv != null",
    "ContractMethodSignatures.isLineNumberList(lineNumbers)"
  })
  public LineNumberingClassAdapter(ClassVisitor cv, List<Long> lineNumbers) {
    super(Opcodes.ASM5, cv);
    this.lineNumbers = lineNumbers;
  }

  /**
   * Visits the specified method, adding line numbering.
   */
  @Override
  public MethodVisitor visitMethod(int access, final String name, String desc,
                                   String signature, String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access | Opcodes.ACC_SYNTHETIC,
                                      name, desc, signature, exceptions);
    return new LineNumberingMethodAdapter(mv, access | Opcodes.ACC_SYNTHETIC,
                                          name, desc) {
      @Override
      protected void onMethodEnter() {
        this.lineNumbers = LineNumberingClassAdapter.this.lineNumbers;
        super.onMethodEnter();
      }
    };
  }
}
