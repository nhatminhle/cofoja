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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

/**
 * A tokenizer that processes Java balanced expressions. In addition
 * to the work of a {@link LineNumberingTokenizer}, a
 * BalancedTokenizer also keeps track of punctuation balancing. It
 * knows about parentheses, square brackets, and braces.
 *
 * <p>It doesn't check uses of angle brackets, because of the added
 * complexity. This is usually not an issue, though, as, in Java, type
 * parameters have a very limited syntax.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({
  "stack != null",
  "!stack.contains(null)"
})
public class BalancedTokenizer extends LineNumberingTokenizer {
  /**
   * Balanced punctuation stack. Opening punctuationm symbols are
   * stored in this stack.
   */
  protected Deque<Character> stack;

  /**
   * Constructs a new BalancedTokenizer reading characters from
   * {@code reader}.
   */
  @Requires("reader != null")
  public BalancedTokenizer(Reader reader) {
    super(reader);
    stack = new ArrayDeque<Character>();
  }

  @Ensures({
    "result != null",
    "result.size() == getCurrentLevel()"
  })
  public Collection<Character> getStack() {
    return Collections.unmodifiableCollection(stack);
  }

  @Ensures({
    "result >= 0",
    "result == getStack().size()"
  })
  public int getCurrentLevel() {
    return stack.size();
  }

  @Requires("\"()[]{}\".indexOf(c) != -1")
  protected char getMatchingDelimiter(char c) {
    switch (c) {
      case '(':
        return ')';
      case '[':
        return ']';
      case '{':
        return '}';
      case ')':
        return '(';
      case ']':
        return '[';
      case '}':
        return '{';
      default:
        throw new IllegalArgumentException();
    }
  }

  @Override
  protected boolean lex() throws IOException {
    if (!super.lex()) {
      if (!stack.isEmpty()) {
        char d = getMatchingDelimiter(stack.pop());
        errorMessage = "'" + d + "' expected";
        hasErrors_ = true;
        return false;
      }
      return false;
    }
    if (nextToken.kind == TokenKind.SYMBOL) {
      char c = nextToken.text.charAt(0);
      switch (c) {
        case '(':
        case '[':
        case '{':
          stack.push(c);
          break;
        case ')':
        case ']':
        case '}':
          String error = null;
          if (stack.isEmpty()) {
            error = "unexpected '" + c + "'";
          } else {
            char d = getMatchingDelimiter(stack.pop());
            if (d != c) {
              error = "'" + d + "' expected";
            }
          }
          if (error != null) {
            unreadChar(c);
            nextToken = null;

            errorMessage = error;
            hasErrors_ = true;
            return false;
          }
          break;
        default:
          break;
      }
    }
    return true;
  }
}
