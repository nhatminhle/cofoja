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

import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A tokenizer that supports look-ahead through a push-back interface.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({
  "queue != null",
  "!queue.contains(null)"
})
public class PushbackTokenizer extends JavaTokenizer {
  /**
   * Pushback queue.
   */
  protected Deque<Token> queue;

  /**
   * Constructs a new PushbackTokenizer reading characters from
   * {@code reader}.
   */
  @Requires("reader != null")
  public PushbackTokenizer(Reader reader) {
    super(reader);
    queue = new ArrayDeque<Token>();
  }

  @Requires("token != null")
  @Ensures("token == getNextToken()")
  public void pushback(Token token) {
    queue.push(token);
  }

  @Override
  public Token getNextToken() {
    Token token = queue.peek();
    if (token != null) {
      return token;
    } else {
      return super.getNextToken();
    }
  }

  @Override
  public int getCurrentOffset() {
    Token token = queue.peek();
    if (token != null) {
      return token.offset;
    } else {
      return super.getCurrentOffset();
    }
  }

  @Override
  public boolean hasNext() {
    if (!queue.isEmpty()) {
      return true;
    } else {
      return super.hasNext();
    }
  }

  @Override
  public Token next() {
    Token token = queue.poll();
    if (token != null) {
      return token;
    } else {
      return super.next();
    }
  }

  /**
   * Returns the next {@code lookahead}-th token, or {@code null} if
   * there is none. Look-ahead tokens are counted from 0.
   */
  @Requires("lookahead >= 0")
  public Token peek(int lookahead) {
    ArrayDeque<Token> tmp = new ArrayDeque<Token>();
    for (int i = 0; i <= lookahead; ++i) {
      if (hasNext()) {
        tmp.push(next());
      }
    }
    Token token = tmp.peek();
    while (!tmp.isEmpty()) {
      pushback(tmp.pop());
    }
    return token;
  }
}
