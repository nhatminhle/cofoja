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
import com.google.java.contract.core.util.JavaUtils;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

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
 */
@Invariant({
  "diagnostics != null",
  "oldId >= 0",
  "oldParameters != null => oldParametersCode != null",
  "oldParameters != null => oldParametersLineNumbers != null",
  "oldParameters != null => !oldParameters.contains(null)",
  "oldParameters != null => !oldParametersCode.contains(null)",
  "oldParameters != null => !oldParametersLineNumbers.contains(null)",
  "oldParameters != null => oldParameters.size() == oldParametersCode.size()",
  "oldParameters != null " +
      "=> oldParameters.size() == oldParametersLineNumbers.size()"
})
public class ContractExpressionTransformer {
  private static final String MAGIC_CAST_METHOD =
      "com.google.java.contract.core.runtime.ContractRuntime.magicCast";

  /**
   * The diagnostic listener to report errors to.
   */
  protected DiagnosticListener<JavaFileObject> diagnostics;

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
   * @param diagnostics listener to report errors to
   * @param acceptOld whether old expressions are recognized
   */
  public ContractExpressionTransformer(
      DiagnosticListener<JavaFileObject> diagnostics, boolean acceptOld) {
    this.diagnostics = diagnostics;
    this.acceptOld = acceptOld;
    oldParameters = null;
    oldParametersCode = null;
    oldParametersLineNumbers = null;
    newCode = null;
    parsed = false;
    oldId = 0;
  }

  public void setAcceptOld(boolean acceptOld) {
    this.acceptOld = acceptOld;
  }

  @Requires({
    "currentBuffer != null",
    "tokenizer != null",
    "token != null"
  })
  @SuppressWarnings("fallthrough")
  private int transformCommon(StringBuilder currentBuffer,
                              BalancedTokenizer tokenizer, Token token) {
    switch (token.kind) {
      case COMMENT:
        int length = token.text.length();
        for (int i = 0; i < length; ++i) {
          currentBuffer.append(" ");
        }
        break;
      case SYMBOL:
        if (token.text.equals("=")
            && tokenizer.hasNext()
            && tokenizer.getNextToken().text.equals(">")) {
          tokenizer.next();
          currentBuffer.append("? ");
          return 1;
        }
        /* Fall through. */
      default:
        currentBuffer.append(token.text);
    }
    return 0;
  }

  @Requires({
    "currentBuffer != null",
    "impliesCount >= 0"
  })
  private void appendImpliesTrail(StringBuilder currentBuffer,
                                  int impliesCount) {
    if (impliesCount > 0) {
      currentBuffer.append(JavaUtils.BEGIN_GENERATED_CODE);
      for (int i = 0; i < impliesCount; ++i) {
        currentBuffer.append(" : true");
      }
      currentBuffer.append(JavaUtils.END_GENERATED_CODE);
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
    "result => newCode.size() == code.size()"
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

      ArrayDeque<Integer> impliesContext = new ArrayDeque<Integer>();
      int impliesCount = 0;

      while (tokenizer.hasNext()) {
        Token token = tokenizer.next();
        newLevel = tokenizer.getCurrentLevel();

        StringBuilder currentBuffer = oldBuffer != null ? oldBuffer : buffer;

        /* Unexpected ';' error. */
        if (newLevel == 0 && token.text.equals(";")) {
          diagnostics.report(new ContractDiagnostic(
              Diagnostic.Kind.ERROR,
              "'\"' expected",
              expr, token.offset, token.offset, token.offset,
              sourceInfo));
          parsed = false;
          continue code;
        }

        /* Implies expressions. */
        if (newLevel < currentLevel) {
          appendImpliesTrail(currentBuffer, impliesCount);
          impliesCount = impliesContext.pop();
        } else if (newLevel > currentLevel) {
          impliesContext.push(impliesCount);
          impliesCount = 0;
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
                  diagnostics.report(new ContractDiagnostic(
                      Diagnostic.Kind.ERROR,
                      "nested old expression",
                      expr, token.offset, token.offset, token.offset,
                      sourceInfo));
                  parsed = false;
                  continue code;
                }
                oldBuffer.append(token.text);
                break;
              default:
                impliesCount += transformCommon(oldBuffer, tokenizer, token);
            }
          }
        } else {
          switch (token.kind) {
            case WORD:
              if (acceptOld && token.text.equals("old")) {
                /* Start of old expression. */
                if (!tokenizer.hasNext()
                    || !tokenizer.next().text.equals("(")) {
                  int errorPos = tokenizer.getCurrentOffset();
                  diagnostics.report(new ContractDiagnostic(
                      Diagnostic.Kind.ERROR,
                      "'(' expected",
                      expr, errorPos, errorPos, errorPos,
                      sourceInfo));
                  parsed = false;
                  continue code;
                }

                /* Compute new old variable name. */
                oldName = JavaUtils.OLD_VARIABLE_PREFIX + oldId++;

                /* Enter old context. */
                oldBuffer = new StringBuilder();
                oldContext = currentLevel;
                break;
              }
              /* Fall through. */
            default:
              impliesCount += transformCommon(buffer, tokenizer, token);
          }
        }

        currentLevel = newLevel;
      }

      /* Top-level implies operators. */
      appendImpliesTrail(buffer, impliesCount);

      /* Parse errors. */
      if (tokenizer.hasErrors()) {
        int errorPos = tokenizer.getCurrentOffset();
        diagnostics.report(new ContractDiagnostic(
            Diagnostic.Kind.ERROR,
            tokenizer.getErrorMessage(),
            expr, errorPos, errorPos, errorPos,
            sourceInfo));
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
