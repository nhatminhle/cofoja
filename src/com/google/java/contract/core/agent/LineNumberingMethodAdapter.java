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
import com.google.java.contract.core.util.JavaUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.List;

/**
 * A method adapter that adds {@link #lineNumber} to the beginning of
 * its instructions.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant(
  "lineNumbers == null || " +
      "ContractMethodSignatures.isLineNumberList(lineNumbers)"
)
abstract class LineNumberingMethodAdapter extends AdviceAdapter {
  /**
   * The line numbers to add to the method. This implementation always
   * sets this field to {@code null}. Child classes must override this
   * value before the call to {@link #onMethodEnter()} in order to get
   * useful results.
   */
  protected List<Long> lineNumbers;

  /**
   * Constructs a new LineNumberingMethodAdapter.
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
  public LineNumberingMethodAdapter(MethodVisitor mv, int access,
                                    String name, String desc) {
    super(Opcodes.ASM5, mv, access, name, desc);
    lineNumbers = null;
  }

  @Override
  protected void onMethodEnter() {
    if (lineNumbers != null && !lineNumbers.isEmpty()) {
      Long lineNumber = lineNumbers.get(0);
      if (lineNumber != null) {
        Label methodStart = new Label();
        mark(methodStart);
        mv.visitLineNumber(lineNumber.intValue(), methodStart);
      }
    }
  }

  @Override
  protected void onMethodExit(int opcode) {
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    /* Ignore original line number information. */
  }

  @Override
  public void visitLocalVariable(String name, String desc, String signature,
                                 Label start, Label end, int index) {
    if (lineNumbers != null) {
      String prefix = JavaUtils.SUCCESS_VARIABLE_PREFIX + "$";
      if (name.startsWith(prefix)) {
        try {
          int no = Integer.parseInt(name.substring(prefix.length()));
          if (no < lineNumbers.size()) {
            Long lineNumber = lineNumbers.get(no);
            if (lineNumber != null) {
              mv.visitLineNumber(lineNumber.intValue(), start);
            }
          }
        } catch (NumberFormatException e) {
          /* Not the variable we are looking for. */
        }
      }
    }
    super.visitLocalVariable(name, desc, signature, start, end, index);
  }
}
