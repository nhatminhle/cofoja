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
package com.google.java.contract.core.util;

import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;

import java.io.IOException;
import java.io.Reader;

/**
 * A tokenizer that keeps track of the current line number.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant("currentLineNumber >= 1")
public class LineNumberingTokenizer extends JavaTokenizer {
  /**
   * The current line number, after the last processed token.
   */
  protected long currentLineNumber;

  /**
   * Constructs a new LineNumberingTokenizer reading characters from
   * {@code reader}.
   */
  @Requires("reader != null")
  public LineNumberingTokenizer(Reader reader) {
    super(reader);
    currentLineNumber = 1;
  }

  @Override
  protected boolean lex() throws IOException {
    if (!super.lex()) {
      return false;
    }
    if (nextToken.kind == TokenKind.SPACE
        || nextToken.kind == TokenKind.COMMENT) {
      int lastIndex = -1;
      for (;;) {
        lastIndex = nextToken.text.indexOf('\n', lastIndex + 1);
        if (lastIndex == -1) {
          break;
        }
        ++currentLineNumber;
      }
    }
    return true;
  }

  @Ensures("result >= 1")
  public long getCurrentLineNumber() {
    return currentLineNumber;
  }
}
