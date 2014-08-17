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

import com.google.java.contract.ContractImport;
import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.regex.Pattern;
import javax.tools.JavaFileObject.Kind;

/**
 * Utility methods related to the Java language.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@ContractImport("com.google.java.contract.core.model.ClassName")
public class JavaUtils {
  /**
   * A parse error; thrown by the parsing functions whenever they read
   * an unexpected token in Java code.
   */
  public static class ParseException extends Exception {
    public ParseException(String msg) {
      super(msg);
    }

    public ParseException(Throwable cause) {
      super(cause);
    }

    public ParseException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

  /**
   * File name extension of contract class files.
   */
  public static final String CONTRACTS_EXTENSION = ".contracts";

  /**
   * File name extension of contracted class files.
   */
  public static final String CONTRACTED_EXTENSION = ".class.contracted";

  /**
   * File name extension of source dependency information files.
   */
  public static final String SOURCE_DEPENDENCY_EXTENSION = ".java.d";

  /**
   * A comment string that marks the beginning of generated code.
   */
  public static final String BEGIN_GENERATED_CODE = "/*[*/";

  /**
   * A comment string that marks the end of generated code.
   */
  public static final String END_GENERATED_CODE = "/*]*/";

  /**
   * A comment delimiter string that marks the beginning of a location
   * comment, used to track code positions in errors.
   */
  public static final String BEGIN_LOCATION_COMMENT = "/*[";

  /**
   * A comment delimiter string that marks the end of a location
   * comment, used to track code positions in errors.
   */
  public static final String END_LOCATION_COMMENT = "]*/";

  /**
   * The name of the <em>user-visible</em> result variable.
   */
  public static final String RESULT_VARIABLE =
      "result";

  /**
   * The name of the <em>user-visible</em> signal variable.
   */
  public static final String SIGNAL_VARIABLE =
      "signal";

  /**
   * The suffix appended to helper class names.
   */
  public static final String HELPER_CLASS_SUFFIX =
      "$com$google$java$contract$H";

  /**
   * The prefix added to synthetic member names.
   */
  public static final String SYNTHETIC_MEMBER_PREFIX =
      "com$google$java$contract$S";

  /**
   * The prefix of all old variable names.
   */
  public static final String OLD_VARIABLE_PREFIX =
      "com$google$java$contract$local$old";

  /**
   * The prefix of all temporary variables that are assigned the value
   * of assertion expressions. Their purpose is to syntactically
   * isolate the user expression in order to get better error
   * messages, and provide markers for debug line numbering.
   */
  public static final String SUCCESS_VARIABLE_PREFIX =
      "com$google$java$contract$local$success";

  /**
   * The prefix of all temporary variables for exceptions thrown during
   * assertion expression evaluation.
   */
  public static final String EXCEPTION_VARIABLE_PREFIX =
      "com$google$java$contract$local$exception";

  /**
   * The name of a temporary variable that keeps track of failed
   * contravariant conditions.
   */
  public static final String ERROR_VARIABLE =
      "com$google$java$contract$local$error";

  /**
   * The name of the parameter holding the object to evaluate the
   * predicates against, in interface helper contract methods.
   */
  public static final String THAT_VARIABLE =
      "com$google$java$contract$local$that";

  /**
   * Returns {@code true} if {@code obj} can be cast to class
   * {@code className}, at run time, using reflection.
   */
  @Requires({
    "obj != null",
    "className != null"
  })
  public static boolean objectIsCastableTo(Object obj, String className) {
    try {
      Class<?> clazz = Class.forName(className);
      return clazz.isAssignableFrom(obj.getClass());
    } catch (Exception e) {
      return false;
    }
  }

  @Requires("className != null")
  public static boolean classExists(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Quotes {@code str} so as to be readily inserted into a Java
   * "multi-line" comment.
   */
  @Requires("str != null")
  @Ensures("result != null")
  public static String quoteComment(String str) {
    return str.replace("/*", "/\\*").replace("*/", "*\\/");
  }

  /**
   * Deletes Java comments from {@code code} and returns the resulting
   * string.
   */
  @Requires("code != null")
  @Ensures("result != null")
  public static String deleteComments(String code) {
    StringBuilder buffer = new StringBuilder();
    JavaTokenizer tokenizer = new JavaTokenizer(new StringReader(code));
    while (tokenizer.hasNext()) {
      JavaTokenizer.Token token = tokenizer.next();
      if (token.kind != JavaTokenizer.TokenKind.COMMENT) {
        buffer.append(token.text);
      }
    }
    return buffer.toString();
  }

  /**
   * Deletes generated Java code from {@code code} and returns the
   * resulting string. Generated code is delimited by matching
   * {@link #BEGIN_DEBUG_COMMENT} and {@link #END_GENERATED_CODE}
   * comments.
   */
  @Requires("code != null")
  @Ensures({
    "result != null",
    "code.length() == result.length() + generatedCodeLength(code)"
  })
  public static String deleteGeneratedCode(String code) {
    StringBuilder buffer = new StringBuilder();
    JavaTokenizer tokenizer = new JavaTokenizer(new StringReader(code));
    boolean ignore = false;
    while (tokenizer.hasNext()) {
      JavaTokenizer.Token token = tokenizer.next();
      if (!ignore) {
        if (token.kind == JavaTokenizer.TokenKind.COMMENT
            && token.text.equals(BEGIN_GENERATED_CODE)) {
          ignore = true;
        } else {
          buffer.append(token.text);
        }
      } else {
        if (token.kind == JavaTokenizer.TokenKind.COMMENT
            && token.text.equals(END_GENERATED_CODE)) {
          ignore = false;
        }
      }
    }
    return buffer.toString();
  }

  /**
   * Returns the number of characters used in {@code code} for
   * generated code.
   *
   * @see #deleteGeneratedCode(String)
   */
  @Requires("code != null")
  @Ensures("result == code.length() - deleteGeneratedCode(code).length()")
  public static int generatedCodeLength(String code) {
    int length = 0;
    JavaTokenizer tokenizer = new JavaTokenizer(new StringReader(code));
    boolean ignore = false;
    while (tokenizer.hasNext()) {
      JavaTokenizer.Token token = tokenizer.next();
      if (!ignore) {
        if (token.kind == JavaTokenizer.TokenKind.COMMENT
            && token.text.equals(BEGIN_GENERATED_CODE)) {
          ignore = true;
          length += token.text.length();
        }
      } else {
        if (token.kind == JavaTokenizer.TokenKind.COMMENT
            && token.text.equals(END_GENERATED_CODE)) {
          ignore = false;
        }
        length += token.text.length();
      }
    }
    return length;
  }

  /**
   * Returns {@code code} with all unqualified identifiers remapped
   * according to {@code map}.
   */
  @Requires({
    "code != null",
    "map != null"
  })
  @Ensures("result != null")
  public static String renameLocalVariables(String code,
                                            Map<String, String> map) {
    StringBuilder buffer = new StringBuilder();
    JavaTokenizer tokenizer = new JavaTokenizer(new StringReader(code));
    boolean qualified = false;
    while (tokenizer.hasNext()) {
      JavaTokenizer.Token token = tokenizer.next();
      if (!qualified && token.kind == JavaTokenizer.TokenKind.WORD) {
        String replacement = map.get(token.text);
        if (replacement != null) {
          buffer.append(replacement);
        } else {
          buffer.append(token.text);
        }
      } else {
        buffer.append(token.text);
      }
      qualified = token.text.equals(".");
    }
    return buffer.toString();
  }

  /**
   * Returns {@code true} if the next token in {@code tokenizer},
   * disregarding whitespace, matches {@code text}.
   */
  @Requires({
    "tokenizer != null",
    "text != null"
  })
  public static boolean lookingAt(PushbackTokenizer tokenizer, String text) {
    JavaTokenizer.Token token1 = tokenizer.peek(0);
    JavaTokenizer.Token token2 = tokenizer.peek(1);
    if (token1 == null) {
      return false;
    }
    if (!token1.text.equals(text)) {
      if (token1.kind != JavaTokenizer.TokenKind.SPACE) {
        return false;
      }
      if (token2 == null) {
        return false;
      }
      if (!token2.text.equals(text)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Advances {@code tokenizer} past the next occurrence of
   * {@code text} (which must match a full token).
   */
  @Requires({
    "tokenizer != null",
    "text != null"
  })
  public static void skipPast(JavaTokenizer tokenizer, String text) {
    while (tokenizer.hasNext() && !tokenizer.next().text.equals(text)) {
    }
  }

  /**
   * Parses a Java fully-qualified identifier and positions
   * {@code tokenizer} on the next token.
   *
   * @param tokenizer the tokenizer to read from
   * @param acceptStar {@code true} if the name can end in {@code .*}
   * @return the parsed identifier
   * @throws ParseException if the tokenizer is not at the start of an
   * identifier
   */
  @Requires("tokenizer != null")
  @Ensures(
    "!acceptStar ? ClassName.isQualifiedName(result)" +
        ": ClassName.isQualifiedName(result)" +
        "|| ClassName.isQualifiedName(result.substring(0, result.length() - 2))"
  )
  public static String parseQualifiedName(JavaTokenizer tokenizer,
                                          boolean acceptStar)
      throws ParseException {
    StringBuilder buffer = new StringBuilder();
    boolean expectWord = true;
   loop:
    while (tokenizer.hasNext()) {
      JavaTokenizer.Token token = tokenizer.getNextToken();
      switch (token.kind) {
        case WORD:
          if (expectWord) {
            buffer.append(token.text);
            expectWord = false;
          } else {
            break loop;
          }
          break;
        case SYMBOL:
          switch (token.text.charAt(0)) {
            case '.':
              if (!expectWord) {
                buffer.append(".");
                expectWord = true;
                break;
              } else {
                break loop;
              }
            case '*':
              if (acceptStar && expectWord) {
                buffer.append("*");
                tokenizer.next();
              }
              break loop;
            default:
              break loop;
          }
          break;
        case SPACE:
          break;
        default:
          break loop;
      }
      tokenizer.next();
    }
    if (buffer.length() == 0) {
      throw new ParseException("next token is not part of an identifier");
    }
    return buffer.toString();
  }

  public static String parseQualifiedName(JavaTokenizer tokenizer)
      throws ParseException {
    return parseQualifiedName(tokenizer, false);
  }

  /**
   * Returns a {@link URLClassLoader} that searches {@code path} and
   * delegates to {@code parent}.
   */
  @Requires("path != null")
  public static URLClassLoader getLoaderForPath(String path,
                                                ClassLoader parent) {
    String[] parts = path.split(Pattern.quote(File.pathSeparator));
    URL[] urls = new URL[parts.length];
    for (int i = 0; i < parts.length; ++i) {
      try {
        urls[i] = new File(parts[i]).toURI().toURL();
      } catch (MalformedURLException e) {
        /* Ignore erroneous paths. */
      }
    }
    if (parent == null) {
      return new URLClassLoader(urls);
    } else {
      return new URLClassLoader(urls, parent);
    }
  }

  @Requires("path != null")
  public static URLClassLoader getLoaderForPath(String path) {
    return getLoaderForPath(path, null);
  }

  /**
   * Reads the class file of the specified class, as a stream. The
   * class file is searched in the following places, in order:
   *
   * <ol>
   * <li>The resources of the current class loader, if any.
   * <li>The resources of the system class loader.
   * </ol>
   *
   * @param loader the class loader used to load resources
   * @param className the class name, in binary format
   * @return the content of the contract class file, as a stream, or
   * {@code null} if none was found
   */
  @Requires("ClassName.isBinaryName(className)")
  public static InputStream getClassInputStream(ClassLoader loader,
                                                String className) {
    String fileName = className + Kind.CLASS.extension;
    URL url;

    if (loader != null) {
      url = loader.getResource(fileName);
      if (url == null) {
        return null;
      } else {
        DebugUtils.info("loader", "found " + url);
        return loader.getResourceAsStream(fileName);
      }
    } else {
      url = ClassLoader.getSystemResource(fileName);
      if (url == null) {
        return null;
      } else {
        DebugUtils.info("loader", "found " + url);
        return ClassLoader.getSystemResourceAsStream(fileName);
      }
    }
  }

  /**
   * Reads the contract class file of the specified class, as a
   * stream. The contract class file is searched in the following
   * places, in order:
   *
   * <ol>
   * <li>The resources of the current class loader, if any.
   * <li>The resources of the system class loader.
   * </ol>
   *
   * @param loader the class loader used to load resources
   * @param className the class name, in binary format
   * @return the content of the contract class file, as a stream, or
   * {@code null} if none was found
   */
  @Requires("ClassName.isBinaryName(className)")
  public static InputStream getContractClassInputStream(ClassLoader loader,
                                                        String className,
                                                        boolean loadHelper) {
    String fileName = className + CONTRACTS_EXTENSION;
    String helperFileName = className + HELPER_CLASS_SUFFIX
        + Kind.CLASS.extension;

    URL url;

    if (loader != null) {
      url = loader.getResource(helperFileName);
      if (url != null && loadHelper) {
        DebugUtils.info("loader", "found " + url);
        return loader.getResourceAsStream(helperFileName);
      }

      url = loader.getResource(fileName);
      if (url != null) {
        DebugUtils.info("loader", "found " + url);
        return loader.getResourceAsStream(fileName);
      }

      return null;
    } else {
      url = ClassLoader.getSystemResource(helperFileName);
      if (url != null && loadHelper) {
        DebugUtils.info("loader", "found " + url);
        return ClassLoader.getSystemResourceAsStream(helperFileName);
      }

      url = ClassLoader.getSystemResource(fileName);
      if (url != null) {
        DebugUtils.info("loader", "found " + url);
        return ClassLoader.getSystemResourceAsStream(fileName);
      }

      return null;
    }
  }

  public static InputStream getContractClassInputStream(ClassLoader loader,
                                                        String className) {
    return getContractClassInputStream(loader, className, false);
  }

  @Requires("className != null")
  public static boolean resourceExists(ClassLoader loader, String className) {
    if (loader != null) {
      return loader.getResource(className) != null;
    } else {
      return ClassLoader.getSystemResource(className) != null;
    }
  }
}
