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
package com.google.java.contract.core.apt;

import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ClassName;
import com.google.java.contract.core.model.ElementKind;
import com.google.java.contract.core.model.VariableModel;
import com.google.java.contract.core.util.BalancedTokenizer;
import com.google.java.contract.core.util.JavaTokenizer.Token;
import com.google.java.contract.core.util.JavaTokenizer.TokenKind;
import com.google.java.contract.core.util.JavaUtils;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A contract expression checker and transformer. An instance of this
 * class can do basic syntax checking and handle language extensions
 * supported in contract expression.
 *
 * <p>Currently, the following checks are performed:
 * <ul>
 * <li>Proper nesting and matching of '()', '[]' and '{}'.
 * <li>Erroneous ';' in expression.
 * </ul>
 *
 * <p>And the following transformations are applied:
 * <ul>
 * <li>Comments are replaced by whitespace.
 * <li>{@code old()} expressions are extracted and replaced with old
 * value variable references. (Optional.)
 * </ul>
 *
 * <p>All generated code is marked up with the appropriate tags, as
 * defined in {@link com.google.java.contract.core.util.JavaUtils}.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author chatain@google.com (Leonardo Chatain)
 */
@Invariant({
  "diagnosticManager != null",
  "oldId >= 0",
  "oldParameters == null || oldParametersCode != null",
  "oldParameters == null || oldParametersLineNumbers != null",
  "oldParameters == null || !oldParameters.contains(null)",
  "oldParameters == null || !oldParametersCode.contains(null)",
  "oldParameters == null || oldParameters.size() == oldParametersCode.size()",
  "oldParameters == null " +
      "|| oldParameters.size() == oldParametersLineNumbers.size()"
})
public class ContractExpressionTransformer {
  private static final String MAGIC_CAST_METHOD =
      "com.google.java.contract.core.runtime.ContractRuntime.magicCast";

  /**
   * The diagnostic manager to report errors to.
   */
  protected DiagnosticManager diagnosticManager;

  /**
   * {@code true} if old expressions should be transformed.
   * If {@code false}, old constructs are ignored.
   */
  protected boolean acceptOld;

  /**
   * The extra parameters needed to hold the extracted old values.
   */
  protected List<VariableModel> oldParameters;

  /**
   * The code corresponding to the old parameters.
   */
  protected List<String> oldParametersCode;

  /**
   * The line numbers corresponding to the old parameters.
   */
  protected List<Long> oldParametersLineNumbers;

  /**
   * The processed code, free of {@code old()} expressions.
   */
  protected List<String> newCode;

  /**
   * Whether the last call to {@link #transform(List<String>,Object)}
   * was successful.
   */
  protected boolean parsed;

  /**
   * The identifier of the next old variable to allocate.
   */
  protected int oldId;

  /**
   * Constructs a new ContractExpressionTransformer.
   *
   * @param diagnosticManager manager to report errors to
   * @param acceptOld whether old expressions are recognized
   */
  public ContractExpressionTransformer(DiagnosticManager diagnosticManager,
                                       boolean acceptOld) {
    this.diagnosticManager = diagnosticManager;
    this.acceptOld = acceptOld;
    oldParameters = null;
    oldParametersCode = null;
    oldParametersLineNumbers = null;
    newCode = null;
    parsed = false;
    oldId = 0;
  }

  @Requires({
    "currentBuffer != null",
    "tokenizer != null",
    "token != null"
  })
  private void transformCommon(StringBuilder currentBuffer,
                              BalancedTokenizer tokenizer, Token token) {
    switch (token.kind) {
      case COMMENT:
        int length = token.text.length();
        for (int i = 0; i < length; ++i) {
          currentBuffer.append(" ");
        }
        break;
      default:
        currentBuffer.append(token.text);
    }
  }

  /**
   * Checks and transforms {@code code}. If successful, results are
   * stored in this instance and can be queried using the appropriate
   * methods.
   *
   * @param code the list of contract expressions to parse
   * @param lineNumbers line numbers associated with {@code code}
   * @param sourceInfo optional source information
   * @return {@code true} if there was no errors
   */
  @Requires({
    "code != null",
    "lineNumbers != null",
    "code.size() == lineNumbers.size()"
  })
  @Ensures({
    "result == canQueryResults()",
    "!result || newCode.size() == code.size()"
  })
  @SuppressWarnings("fallthrough")
  public boolean transform(List<String> code, List<Long> lineNumbers,
                           Object sourceInfo) {
    oldParameters = new ArrayList<VariableModel>();
    oldParametersCode = new ArrayList<String>();
    oldParametersLineNumbers = new ArrayList<Long>();
    newCode = new ArrayList<String>();
    parsed = true;

    Iterator<Long> iterLineNumber = lineNumbers.iterator();

   code:
    for (String expr : code) {
      Long lineNumber = iterLineNumber.hasNext() ? iterLineNumber.next() : null;

      BalancedTokenizer tokenizer =
          new BalancedTokenizer(new StringReader(expr));
      int currentLevel = 0;
      int newLevel = 0;

      StringBuilder buffer = new StringBuilder();

      StringBuilder oldBuffer = null;
      String oldName = null;
      int oldContext = -1;

      while (tokenizer.hasNext()) {
        Token token = tokenizer.next();
        newLevel = tokenizer.getCurrentLevel();

        StringBuilder currentBuffer = oldBuffer != null ? oldBuffer : buffer;

        /* Unexpected ';' error. */
        if (newLevel == 0 && token.text.equals(";")) {
          diagnosticManager.error("'\"' expected",
              expr, token.offset, token.offset, token.offset,
              sourceInfo);
          parsed = false;
          continue code;
        }

        /* old expressions. */
        if (oldBuffer != null) {
          if (newLevel == oldContext) {
            /* End of old expression. */
            String oldExpr = oldBuffer.toString();
            oldParameters.add(
                new VariableModel(ElementKind.PARAMETER, oldName,
                                  new ClassName("java/lang/Object")));
            oldParametersCode.add(oldExpr);
            oldParametersLineNumbers.add(lineNumber);

            /* Pad buffer (for error reporting purposes). */
            buffer.append("(   ");

            /* Replace old expression in original expression. */
            buffer.append(JavaUtils.BEGIN_GENERATED_CODE);
            buffer.append(MAGIC_CAST_METHOD);
            buffer.append("(");
            buffer.append(oldName);
            buffer.append(", ");
            buffer.append("true ? null : ");
            buffer.append(JavaUtils.END_GENERATED_CODE);
            buffer.append(oldExpr);
            buffer.append(JavaUtils.BEGIN_GENERATED_CODE);
            buffer.append(")");
            buffer.append(JavaUtils.END_GENERATED_CODE);

            /* Pad buffer (for error reporting purposes). */
            buffer.append(")");

            /* Exit old context. */
            oldBuffer = null;
            oldContext = -1;
          } else {
            switch (token.kind) {
              case WORD:
                if (token.text.equals("old")) {
                  diagnosticManager.error("nested old expression",
                      expr, token.offset, token.offset, token.offset,
                      sourceInfo);
                  parsed = false;
                  continue code;
                }
                oldBuffer.append(token.text);
                break;
              default:
                transformCommon(oldBuffer, tokenizer, token);
            }
          }
        } else {
          switch (token.kind) {
            case WORD:
              if (acceptOld && token.text.equals("old")) {
                /* Start of old expression. */
                Token afterOld = null;
                if (!tokenizer.hasNext()
                    || !((afterOld = tokenizer.next()).text.equals("(")
                         || (afterOld.kind == TokenKind.SPACE
                             && tokenizer.hasNext()
                             && tokenizer.next().text.equals("(")))) {
                  int errorPos = afterOld != null ? afterOld.offset
                      : tokenizer.getCurrentOffset();
                  diagnosticManager.error("'(' expected",
                      expr, errorPos, errorPos, errorPos,
                      sourceInfo);
                  parsed = false;
                  continue code;
                }

                /* Compute new old variable name. */
                oldName = JavaUtils.OLD_VARIABLE_PREFIX + oldId++;

                /* Enter old context. */
                if (afterOld.kind == TokenKind.SPACE) {
                  oldBuffer = new StringBuilder(afterOld.text);
                } else {
                  oldBuffer = new StringBuilder();
                }
                oldContext = currentLevel;
                break;
              }
              /* Fall through. */
            default:
              transformCommon(buffer, tokenizer, token);
          }
        }

        currentLevel = newLevel;
      }

      /* Parse errors. */
      if (tokenizer.hasErrors()) {
        int errorPos = tokenizer.getCurrentOffset();
        diagnosticManager.error(tokenizer.getErrorMessage(),
            expr, errorPos, errorPos, errorPos, sourceInfo);
        parsed = false;
        continue code;
      }
      newCode.add(buffer.toString());
    }

    return parsed;
  }

  /**
   * Returns {@code true} if results are ready to be queried.
   */
  public boolean canQueryResults() {
    return parsed;
  }

  @Ensures("result >= 0")
  public int getNextOldId() {
    return oldId;
  }

  @Requires("canQueryResults()")
  @Ensures({
    "result != null",
    "result.size() == getOldParametersCode().size()",
    "result.size() == getOldParametersLineNumbers().size()"
  })
  public List<VariableModel> getOldParameters() {
    return oldParameters;
  }

  @Requires("canQueryResults()")
  @Ensures({
    "result != null",
    "result.size() == getOldParameters().size()",
    "result.size() == getOldParametersLineNumbers().size()"
  })
  public List<String> getOldParametersCode() {
    return oldParametersCode;
  }

  @Requires("canQueryResults()")
  @Ensures({
    "result != null",
    "result.size() == getOldParameters().size()",
    "result.size() == getOldParametersCode().size()"
  })
  public List<Long> getOldParametersLineNumbers() {
    return oldParametersLineNumbers;
  }

  @Requires("canQueryResults()")
  @Ensures("result != null")
  public List<String> getTransformedCode() {
    return newCode;
  }
}
