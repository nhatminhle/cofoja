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
import java.io.PushbackReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A tokenizer that processes Java expressions. It knows about words,
 * quoted strings, comments, and white space. Symbols are <em>not</em>
 * aggregated into operators.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant("reader != null")
public class JavaTokenizer implements Iterator<JavaTokenizer.Token> {
  /**
   * The kind of a token.
   */
  public static enum TokenKind {
    /**
     * An identifier or keyword.
     */
    WORD,

    /**
     * A quoted literal.
     */
    QUOTE,

    /**
     * A punctuation symbol.
     */
    SYMBOL,

    /**
     * A comment.
     */
    COMMENT,

    /**
     * A sequence of whitespace.
     */
    SPACE;
  }

  /**
   * A token returned by an instance of JavaTokenizer.
   */
  public class Token {
    public final TokenKind kind;
    public final String text;
    public final int offset;

    protected Token(TokenKind kind, String text, int offset) {
      this.kind = kind;
      this.text = text;
      this.offset = offset;
    }
  }

  /**
   * The source of characters to tokenize.
   */
  protected PushbackReader reader;

  /**
   * The next token returned by this tokenizer, or {@code null} if
   * none (end of file or not read yet).
   */
  protected Token nextToken;

  /**
   * {@code true} if an error was found during tokenization.
   */
  protected boolean hasErrors_;

  /**
   * The error message, if an error has occurred. Can be {@code null}.
   */
  protected String errorMessage;

  /**
   * The number of characters read by this tokenizer.
   */
  protected int currentOffset;

  /**
   * Constructs a new JavaTokenizer reading characters from
   * {@code reader}.
   */
  @Requires("reader != null")
  public JavaTokenizer(Reader reader) {
    this.reader = new PushbackReader(reader);
    nextToken = null;
    hasErrors_ = false;
    currentOffset = 0;
  }

  /**
   * Reads a character from {@code reader}. In this implementation,
   * all read accesses to the reader pass through this method.
   *
   * @param allowEOF if {@code false}, throws IOException if EOF is
   * reached
   */
  @Ensures({
    "!allowEOF ? result >= 0 : result >= -1"
  })
  protected int readChar(boolean allowEOF) throws IOException {
    int c = reader.read();
    if (c != -1) {
      ++currentOffset;
    } else {
      if (!allowEOF) {
        throw new IOException();
      }
    }
    return c;
  }

  /**
   * Pushes back a character into {@code reader}. In this
   * implementation, all unread accesses to the reader pass through
   * this method.
   */
  protected void unreadChar(int c) throws IOException {
    if (c == -1) {
      return;
    }
    --currentOffset;
    reader.unread(c);
  }

  /**
   * Reads the next token into {@code nextToken}.
   *
   * @return {@code true} if a new token was successfully read
   */
  @Requires("nextToken == null")
  @Ensures("result == (nextToken != null)")
  protected boolean lex() throws IOException {
    int startOffset = currentOffset;
    StringBuilder buffer = new StringBuilder();
    int c = readChar(true);
    if (c == -1) {
      return false;
    }
    buffer.append((char) c);
    switch (c) {
      case '/':
        /* Comments. */
        c = readChar(true);
        switch (c) {
          case '/':
            buffer.append((char) c);
            do {
              c = readChar(false);
              buffer.append((char) c);
            } while (c != '\n');
            nextToken = new Token(TokenKind.COMMENT, buffer.toString(),
                                  startOffset);
            break;
          case '*':
            buffer.append((char) c);
            for (;;) {
              c = readChar(false);
              buffer.append((char) c);
              if (c == '*') {
                c = readChar(false);
                buffer.append((char) c);
                if (c == '/') {
                  break;
                }
              }
            }
            nextToken = new Token(TokenKind.COMMENT, buffer.toString(),
                                  startOffset);
            break;
          default:
            unreadChar(c);
            nextToken = new Token(TokenKind.SYMBOL, buffer.toString(),
                                  startOffset);
        }
        break;

      case '\'':
      case '"':
        /* Quoted strings. */
        int delim = c;
        for (;;) {
          c = readChar(false);
          buffer.append((char) c);
          if (c == delim) {
            break;
          }
          if (c == '\\') {
            buffer.append((char) readChar(false));
          }
        }
        nextToken = new Token(TokenKind.QUOTE, buffer.toString(), startOffset);
        break;

      default:
        if (Character.isJavaIdentifierStart(c)) {
          /* Identifiers. */
          while ((c = readChar(true)) != -1
                 && Character.isJavaIdentifierPart(c)) {
            buffer.append((char) c);
          }
          unreadChar(c);
          nextToken = new Token(TokenKind.WORD, buffer.toString(), startOffset);
        } else if (Character.isWhitespace(c)) {
          /* White space. */
          while ((c = readChar(true)) != -1 && Character.isWhitespace(c)) {
            buffer.append((char) c);
          }
          unreadChar(c);
          nextToken = new Token(TokenKind.SPACE, buffer.toString(),
                                startOffset);
        } else {
          /* Symbol. */
          nextToken = new Token(TokenKind.SYMBOL, buffer.toString(),
                                startOffset);
        }
    }
    return true;
  }

  /**
   * Returns the next token without consuming it.
   */
  public Token getNextToken() {
    if (nextToken == null) {
      try {
        if (!lex()) {
          throw new NoSuchElementException();
        }
      } catch (IOException e) {
        errorMessage = e.getMessage();
        hasErrors_ = true;
        throw new NoSuchElementException();
      }
    }
    return nextToken;
  }

  public int getCurrentOffset() {
    return currentOffset;
  }

  /**
   * Returns {@code true} if an error has been found.
   */
  public boolean hasErrors() {
    return hasErrors_;
  }

  @Requires("hasErrors()")
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public boolean hasNext() {
    if (nextToken != null) {
      return true;
    } else {
      try {
        return lex();
      } catch (IOException e) {
        errorMessage = e.getMessage();
        hasErrors_ = true;
        return false;
      }
    }
  }

  @Override
  public Token next() {
    Token token = getNextToken();
    nextToken = null;
    return token;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
