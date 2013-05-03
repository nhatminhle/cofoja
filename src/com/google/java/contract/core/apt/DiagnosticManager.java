/*
 * Copyright 2010, 2011 Nhat Minh Lê
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * A collection of diagnostic messages with facilities to manage
 * messaging and error reporting.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class DiagnosticManager
    implements DiagnosticListener<JavaFileObject>,
        Iterable<DiagnosticManager.Report> {
  /**
   * An object that can represent heterogeneous kinds of diagnostics,
   * with query methods suitable for use with
   * {@link javax.annotation.processing.Messager}.
   *
   * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
   */
  @Invariant({
    "getKind() != null",
    "getMessage(null) != null",
    "getAnnotationMirror() == null || getElement() != null",
    "getAnnotationValue() == null || getAnnotationMirror() != null"
  })
  public abstract class Report {
    /**
     * Returns the kind of this diagnostic.
     */
    public abstract Kind getKind();

    /**
     * Returns a error message localized according to {@code locale},
     * or the default locale if {@code null}.
     */
    public abstract String getMessage(Locale locale);

    /**
     * Returns the Java annotation processing model element
     * associated with this diagnostic, if any.
     */
    public abstract Element getElement();

    /**
     * Returns the Java annotation processing model annotation mirror
     * associated with this diagnostic, if any.
     */
    public abstract AnnotationMirror getAnnotationMirror();

    /**
     * Returns the Java annotation processing model annotation value
     * associated with this diagnostic.
     */
    public abstract AnnotationValue getAnnotationValue();

    /**
     * Appends to {@code buffer} a string with the faulty part
     * underlined.
     */
    @Requires({
      "buffer != null",
      "expr != null",
      "pos >= start",
      "pos <= end",
      "start >= 0",
      "start <= end",
      "end <= expr.length()"
    })
    protected void underlineError(StringBuilder buffer, String expr,
                                  int pos, int start, int end) {
      int i = 0;
      for (; i < start; ++i) {
        buffer.append(" ");
      }

      if (pos == start) {
        buffer.append("^");
      } else {
        for (; i < pos; ++i) {
          buffer.append("~");
        }
        buffer.append("^");
        ++i;
        for (; i < end; ++i) {
          buffer.append("~");
        }
      }
    }
  }

  /**
   * A diagnostic fired by the annotation processor or one of its
   * components.
   *
   * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
   */
  public class AnnotationReport extends Report {
    protected Kind kind;
    protected String message;
    protected String sourceString;
    protected int position;
    protected int startPosition;
    protected int endPosition;
    protected Element sourceElement;
    protected AnnotationMirror annotationMirror;
    protected AnnotationValue annotationValue;

    /**
     * Constructs a new ContractDiagnostic.
     *
     * @param kind the kind of this diagnostic
     * @param message the message of this diagnostic
     * @param sourceString the code of the contract
     * @param position the position of the error
     * @param startPosition the start position of the error
     * @param endPosition the end position of the error
     * @param sourceElement the source of this diagnostic
     * @param annotationMirror the source annotation of this diagnostic
     * @param annotationValue the source annotation value of this diagnostic
     */
    @Requires({
      "kind != null",
      "message != null",
      "position >= startPosition",
      "position <= endPosition",
      "startPosition >= 0",
      "startPosition <= endPosition"
    })
    @Ensures({
      "kind == getKind()"
    })
    public AnnotationReport(Kind kind, String message, String sourceString,
        int position, int startPosition, int endPosition,
        Element sourceElement,
        AnnotationMirror annotationMirror, AnnotationValue annotationValue) {
      this.kind = kind;
      this.message = message;
      this.sourceString = sourceString;
      this.position = position;
      this.startPosition = startPosition;
      this.endPosition = endPosition;
      this.sourceElement = sourceElement;
      this.annotationMirror = annotationMirror;
      this.annotationValue = annotationValue;
    }

    /**
     * Constructs a new ContractDiagnostic.
     *
     * @param kind the kind of this diagnostic
     * @param message the message of this diagnostic
     * @param sourceString the code of the contract
     * @param position the position of the error
     * @param startPosition the start position of the error
     * @param endPosition the end position of the error
     * @param info the source of this diagnostic
     */
    @Requires({
      "kind != null",
      "message != null",
      "position >= startPosition",
      "position <= endPosition",
      "startPosition >= 0",
      "startPosition <= endPosition"
    })
    @Ensures({
      "kind == getKind()"
    })
    public AnnotationReport(Kind kind, String message, String sourceString,
        int position, int startPosition, int endPosition, Object info) {
      this(kind, message, sourceString, position, startPosition, endPosition,
           null, null, null);
      if (info instanceof AnnotationSourceInfo) {
        AnnotationSourceInfo sourceInfo = (AnnotationSourceInfo) info;
        sourceElement = sourceInfo.getElement();
        annotationMirror = sourceInfo.getAnnotationMirror();
        annotationValue = sourceInfo.getAnnotationValue();
      }
    }

    @Override
    public Kind getKind() {
      return kind;
    }

    @Override
    public String getMessage(Locale locale) {
      if (sourceString == null) {
        return message;
      }
      StringBuilder buffer = new StringBuilder("clause: ");
      buffer.append(sourceString);
      buffer.append("\n        ");
      underlineError(buffer, sourceString, position,
                     startPosition, endPosition);
      return message + "\n" + buffer.toString();
    }

    @Override
    public Element getElement() {
      return sourceElement;
    }

    @Override
    public AnnotationMirror getAnnotationMirror() {
      return annotationMirror;
    }

    @Override
    public AnnotationValue getAnnotationValue() {
      return annotationValue;
    }
  }

  /**
   * A diagnostic issued by an underlying compiler invocation.
   *
   * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
   */
  @Invariant("diagnostic != null")
  public class CompilerReport extends Report {
    protected Diagnostic<? extends JavaFileObject> diagnostic;

    /**
     * Constructs a new CompilerReport based on
     * {@code diagnostic}.
     */
    @Requires("diagnostic != null")
    public CompilerReport(
        Diagnostic<? extends JavaFileObject> diagnostic) {
      this.diagnostic = diagnostic;
    }

    /**
     * Formats a pretty-printed snippet around where the error
     * occurred. The returned snippet has the faulty sequence
     * underlined, if possible.
     */
    @Requires({
      "sourceContent != null",
      "sourceInfo != null"
    })
    protected String formatErrorSnippet(CharSequence sourceContent,
                                        AnnotationSourceInfo sourceInfo) {
      List<String> code = sourceInfo.getCode();
      int column = (int) diagnostic.getColumnNumber();

      /*
       * Determine length of extra context to ensure we match the code
       * expressions entirely.
       */
      int maxCodeLength = 0;
      for (String expr : code) {
        int length = expr.length();
        if (length > maxCodeLength) {
          maxCodeLength = length;
        }
      }

      /* Fetch context. */
      int errorPos = (int) diagnostic.getPosition();
      int lineStart = errorPos - column + 1;
      int errorStart = (int) diagnostic.getStartPosition();
      int errorEnd = (int) diagnostic.getEndPosition();
      String partialLine = sourceContent
          .subSequence(lineStart, errorEnd)
          .toString();

      /* Match failed expression. */
      String snippet = null;
      int snippetErrorPos = -1;
      int snippetErrorStart = -1;
      int snippetErrorEnd = -1;
      for (String expr : code) {
        /* Find debug marker. */
        String commentMarker =
            JavaUtils.BEGIN_LOCATION_COMMENT
            + JavaUtils.quoteComment(expr)
            + JavaUtils.END_LOCATION_COMMENT;
        int pos = partialLine.lastIndexOf(commentMarker);
        if (pos != -1) {
          snippet = expr;
          pos += commentMarker.length();

          /* Compute generated code offsets. */
          int base = lineStart + pos;
          snippetErrorPos = errorPos - base;
          snippetErrorStart = errorStart - base;
          snippetErrorEnd = errorEnd - base;

          snippetErrorPos -= JavaUtils.generatedCodeLength(
              partialLine.substring(pos, pos + snippetErrorPos));
          snippetErrorStart -= JavaUtils.generatedCodeLength(
              partialLine.substring(pos, pos + snippetErrorStart));
          snippetErrorEnd -= JavaUtils.generatedCodeLength(
              partialLine.substring(pos, pos + snippetErrorEnd));
        }
      }

      if (snippet != null) {
        StringBuilder buffer = new StringBuilder("clause: ");
        buffer.append(snippet);
        if (snippetErrorPos != -1) {
          buffer.append("\n        ");
          int end = snippet.length();
          if (snippetErrorPos > end) {
            snippetErrorPos = end;
          }
          if (snippetErrorStart > end) {
            snippetErrorStart = end;
          }
          if (snippetErrorEnd > end) {
            snippetErrorEnd = end;
          }
          underlineError(buffer, snippet, snippetErrorPos,
                         snippetErrorStart, snippetErrorEnd);
          snippet = buffer.toString();
        }
      }
      return snippet;
    }

    /**
     * Returns the {@link AnnotationSourceInfo} object associated with
     * the underlying {@link Diagnostic}, if any.
     */
    protected AnnotationSourceInfo getSourceInfo() {
      JavaFileObject source = diagnostic.getSource();
      if (!(source instanceof SyntheticJavaFile)) {
        return null;
      }
      SyntheticJavaFile synthSource = (SyntheticJavaFile) source;
      Object info = synthSource.getSourceInfo(diagnostic.getLineNumber());
      if (!(info instanceof AnnotationSourceInfo)) {
        return null;
      }
      return (AnnotationSourceInfo) info;
    }

    @Override
    public Kind getKind() {
      return diagnostic.getKind();
    }

    @Override
    public String getMessage(Locale locale) {
      AnnotationSourceInfo sourceInfo = getSourceInfo();
      String msg = "error in contract: ";

      /*
       * This translates confusing "'<token>' expected" messages from
       * javac to plain "syntax error" messages.
       *
       * TODO(lenh): Think of a more generic way to handle
       * compiler-specific error message rewriting.
       */
      String errorCode = diagnostic.getCode();
      if (errorCode != null
          && errorCode.startsWith("compiler.err.expected")) {
        msg += "syntax error";
      } else {
        msg += diagnostic.getMessage(locale);
      }

      JavaFileObject source = diagnostic.getSource();
      if (source != null && sourceInfo != null) {
        try {
          CharSequence chars = source.getCharContent(true);
          return msg + "\n" + formatErrorSnippet(chars, sourceInfo);
        } catch (IOException e) {
          /* No source code available. */
        }
      }

      return msg;
    }

    @Override
    public Element getElement() {
      AnnotationSourceInfo sourceInfo = getSourceInfo();
      return getSourceInfo() == null ? null : sourceInfo.getElement();
    }

    @Override
    public AnnotationMirror getAnnotationMirror() {
      AnnotationSourceInfo sourceInfo = getSourceInfo();
      return getSourceInfo() == null
          ? null : sourceInfo.getAnnotationMirror();
    }

    @Override
    public AnnotationValue getAnnotationValue() {
      AnnotationSourceInfo sourceInfo = getSourceInfo();
      return getSourceInfo() == null
          ? null : sourceInfo.getAnnotationValue();
    }
  }

  protected List<Report> reports;
  protected int errorCount;

  public DiagnosticManager() {
    reports = new ArrayList<Report>();
    errorCount = 0;
  }

  public boolean hasErrors() {
    return errorCount != 0;
  }

  @Ensures("result >= 0")
  public int getErrorCount() {
    return errorCount;
  }

  @Ensures("result >= 0")
  public int getCount() {
    return reports.size();
  }

  @Override
  public Iterator<Report> iterator() {
    return reports.iterator();
  }

  /**
   * Adds {@code r} to the list of reports of this manager.
   */
  @Requires("r != null")
  public void report(Report r) {
    if (r.getKind() == Kind.ERROR) {
      ++errorCount;
    }
    reports.add(r);
  }

  /**
   * Reports a compiler diagnostic. This diagnostic manager only
   * reports errors from the underlying compiler tool invocation,
   * which calls this method; anything other than an error is
   * ignored. This prevents the annotation processor from picking up
   * irrelevant warnings pertaining to generated code.
   */
  @Override
  public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
    if (diagnostic.getKind() != Kind.ERROR)
      return;
    report(new CompilerReport(diagnostic));
  }

  /**
   * Reports an error.
   */
  public void error(String message, String sourceString,
                    int position, int startPosition, int endPosition,
                    Object info) {
    report(new AnnotationReport(Kind.ERROR, message, sourceString,
                                position, startPosition, endPosition, info));
  }

  public void error(String message, String sourceString,
                    int position, int startPosition, int endPosition,
                    Element sourceElement, AnnotationMirror annotationMirror,
                    AnnotationValue annotationValue) {
    report(new AnnotationReport(Kind.ERROR, message, sourceString,
                                position, startPosition, endPosition,
                                sourceElement, annotationMirror,
                                annotationValue));
  }

  /**
   * Reports a warning.
   */
  public void warning(String message, String sourceString,
                      int position, int startPosition, int endPosition,
                      Object info) {
    report(new AnnotationReport(Kind.WARNING, message, sourceString,
                                position, startPosition, endPosition, info));
  }

  public void warning(String message, String sourceString,
                      int position, int startPosition, int endPosition,
                      Element sourceElement, AnnotationMirror annotationMirror,
                      AnnotationValue annotationValue) {
    report(new AnnotationReport(Kind.WARNING, message, sourceString,
                                position, startPosition, endPosition,
                                sourceElement, annotationMirror,
                                annotationValue));
  }
}
