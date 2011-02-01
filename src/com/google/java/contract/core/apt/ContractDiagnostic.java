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
package com.google.java.contract.core.apt;

import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.util.JavaUtils;
import com.google.java.contract.core.util.SyntheticJavaFile;

import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * A diagnostic related to contract code.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
@Invariant({
  "getSource() != null",
  "getPosition() >= startPosition",
  "getPosition() <= endPosition",
  "getStartPosition() >= 0",
  "getStartPosition() <= getEndPosition()"
})
public class ContractDiagnostic extends SimpleDiagnostic {
  protected SyntheticJavaFile sourceFile;
  protected int position;
  protected int startPosition;
  protected int endPosition;
  protected Object sourceInfo;

  /**
   * Constructs a new ContractDiagnostic.
   *
   * @param kind the kind of this diagnostic
   * @param message the message of this diagnostic
   * @param sourceString the code of the contract
   * @param position the position of the error
   * @param startPosition the start position of the error
   * @param endPosition the end position of the error
   * @param sourceInfo the source of this diagnostic
   */
  @Requires({
    "kind != null",
    "message != null",
    "sourceString != null",
    "position >= startPosition",
    "position <= endPosition",
    "startPosition >= 0",
    "startPosition <= endPosition"
  })
  @Ensures({
    "kind == getKind()",
    "message.equals(getMessage(null))"
  })
  public ContractDiagnostic(Kind kind, String message, String sourceString,
                            int position, int startPosition, int endPosition,
                            Object sourceInfo) {
    super(kind, message);
    String commentMarker =
        JavaUtils.BEGIN_LOCATION_COMMENT
        + JavaUtils.quoteComment(sourceString)
        + JavaUtils.END_LOCATION_COMMENT;
    String code = commentMarker + sourceString;
    sourceFile = new SyntheticJavaFile("com.google.java.contract.null",
                                       code.getBytes(), null);
    int base = commentMarker.length();
    this.position = base + position;
    this.startPosition = base + startPosition;
    this.endPosition = base + endPosition;
    this.sourceInfo = sourceInfo;
  }

  @Override
  public long getColumnNumber() {
    return getPosition() + 1;
  }

  @Override
  public long getEndPosition() {
    return endPosition;
  }

  @Override
  public long getLineNumber() {
    return 1;
  }

  @Override
  public long getPosition() {
    return position;
  }

  @Override
  public JavaFileObject getSource() {
    return sourceFile;
  }

  @Override
  public long getStartPosition() {
    return startPosition;
  }

  public Object getSourceInfo() {
    return sourceInfo;
  }
}
