/*
 * Copyright 2010 Nhat Minh Lê
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

import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * A simple diagnostic not tied to any particular source.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({
  "getKind() != null",
  "getMessage(null) != null"
})
public class SimpleDiagnostic implements Diagnostic<JavaFileObject> {
  protected Kind kind;
  protected String message;

  /**
   * Constructs a new SimpleDiagnostic.
   *
   * @param kind the kind of this diagnostic
   * @param message the message of this diagnostic
   */
  @Requires({
    "kind != null",
    "message != null"
  })
  @Ensures({
    "kind == getKind()",
    "message.equals(getMessage(null))"
  })
  public SimpleDiagnostic(Kind kind, String message) {
    this.kind = kind;
    this.message = message;
  }

  @Override
  public Kind getKind() {
    return kind;
  }

  @Override
  public String getMessage(Locale locale) {
    return message;
  }

  @Override
  public String getCode() {
    return null;
  }

  @Override
  public long getColumnNumber() {
    return NOPOS;
  }

  @Override
  public long getEndPosition() {
    return NOPOS;
  }

  @Override
  public long getLineNumber() {
    return NOPOS;
  }

  @Override
  public long getPosition() {
    return NOPOS;
  }

  @Override
  public JavaFileObject getSource() {
    return null;
  }

  @Override
  public long getStartPosition() {
    return NOPOS;
  }
}
