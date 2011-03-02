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
package com.google.java.contract.core.model;

import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.util.Predicate;

/**
 * A class name. This class supports the following formats:
 *
 * <ul>
 * <li>simple: without package or nesting prefix;
 * <li>(fully) qualified: with package prefix and enclosing classes
 * separated with dots;
 * <li>semi-qualified: with dot-separated package prefix (any '$' from
 * class nesting remains);
 * <li>declared: same as qualified, plus generic parameters;
 * <li>binary: as found in bytecode (slash-separated package prefix).
 * </ul>
 *
 * <p>For primitive and array types, use {@link TypeName}.
 *
 * <p>Anonymous classes do not have a proper name, and so cannot be
 * designated by objects of this class.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({
  "isSimpleName(getSimpleName())",
  "isQualifiedName(getQualifiedName())",
  "isQualifiedName(getSemiQualifiedName())",
  "isBinaryName(getBinaryName())"
})
public class ClassName extends TypeName {
  protected String simpleName;
  protected String qualifiedName;
  protected String semiQualifiedName;
  protected String binaryName;

  /**
   * Constructs a new ClassName from its binary name. Other forms are
   * inferred. The resulting class name is not generic (has no generic
   * parameter).
   */
  @Requires("isBinaryName(binaryName)")
  @Ensures("binaryName.equals(getBinaryName())")
  public ClassName(String binaryName) {
    this.binaryName = binaryName;
    simpleName = null;
    inferSemiQualifiedName();
    inferQualifiedName();
    inferSimpleName();
    declaredName = qualifiedName;
  }

  /**
   * Constructs a new ClassName from its binary and declared
   * names. Other forms are inferred. The binary and declared names
   * must represent the same type.
   */
  @Requires({
    "isBinaryName(binaryName)",
    "declaredName != null"
  })
  @Ensures({
    "binaryName.equals(getBinaryName())",
    "declaredName.equals(getDeclaredName())"
  })
  public ClassName(String binaryName, String declaredName) {
    this.binaryName = binaryName;
    this.declaredName = declaredName;
    simpleName = null;
    inferSemiQualifiedName();
    inferQualifiedName();
    inferSimpleName();
    assertDeclaredQualifiedMatch();
  }

  @Requires({
    "binaryName != null",
    "declaredName != null",
    "simpleName != null",
    "binaryName.endsWith(simpleName)"
  })
  @Ensures({
    "binaryName.equals(getBinaryName())",
    "declaredName.equals(getDeclaredName())",
    "simpleName.equals(getSimpleName())"
  })
  public ClassName(String binaryName, String declaredName, String simpleName) {
    this.binaryName = binaryName;
    this.declaredName = declaredName;
    this.simpleName = simpleName;
    inferSemiQualifiedName();
    inferQualifiedName();
    assertDeclaredQualifiedMatch();
  }

  protected void assertDeclaredQualifiedMatch() {
    if (!declaredName.replaceAll("<[^.]*>", "").startsWith(qualifiedName)) {
      throw new IllegalArgumentException(
          "declared name '" + declaredName
          + "' does not match qualified name '" + qualifiedName + "'");
    }
  }

  public String getSimpleName() {
    return simpleName;
  }

  /**
   * Returns the relative component of {@code pathName}.
   *
   * @param pathName a dot-separated path
   */
  @Requires("pathName != null")
  @Ensures("result != null")
  public static String getRelativeName(String pathName) {
    int lastSep = pathName.lastIndexOf('.');
    if (lastSep == -1) {
      return pathName;
    } else {
      return pathName.substring(lastSep + 1);
    }
  }

  /**
   * Returns the package component of {@code pathName}.
   *
   * @param pathName a dot-separated path
   */
  @Requires("pathName != null")
  @Ensures("result != null")
  public static String getPackageName(String pathName) {
    int lastSep = pathName.lastIndexOf('.');
    if (lastSep == -1) {
      return "";
    } else {
      return pathName.substring(0, lastSep);
    }
  }

  public String getQualifiedName() {
    return qualifiedName;
  }

  public String getSemiQualifiedName() {
    return semiQualifiedName;
  }

  public String getBinaryName() {
    return binaryName;
  }

  private void inferSemiQualifiedName() {
    semiQualifiedName = binaryName.replace('/', '.');
  }

  private void inferQualifiedName() {
    if (simpleName == null) {
      qualifiedName = semiQualifiedName.replace('$', '.');
    } else {
      int prefixLength = semiQualifiedName.length() - simpleName.length();
      String prefix = semiQualifiedName.substring(0, prefixLength);
      qualifiedName = prefix.replace('$', '.') + simpleName;
    }
  }

  private void inferSimpleName() {
    int i = qualifiedName.lastIndexOf('.');
    if (i == -1) {
      simpleName = qualifiedName;
    } else {
      simpleName = qualifiedName.substring(i + 1);
    }
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ClassName
        && binaryName.equals(((ClassName) obj).binaryName)
        && declaredName.equals(((ClassName) obj).declaredName);
  }

  @Override
  public int hashCode() {
    return binaryName.hashCode() ^ declaredName.hashCode();
  }

  /**
   * Returns {@code true} if {@code name} is <em>syntactically</em> a
   * valid simple name.
   */
  public static boolean isSimpleName(String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }
    if (!Character.isJavaIdentifierStart(name.charAt(0))) {
      return false;
    }
    int len = name.length();
    for (int i = 1; i < len; ++i) {
      if (!Character.isJavaIdentifierPart(name.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  protected static final Predicate<String> IS_SIMPLE_NAME =
      new Predicate<String>() {
    @Override
    public boolean apply(String name) {
      return isSimpleName(name);
    }
  };

  public static Predicate<String> isSimpleName() {
    return IS_SIMPLE_NAME;
  }

  /**
   * Returns {@code true} if {@code name} is <em>syntactically</em> a
   * valid qualified name.
   *
   * <p>Note: there is no syntactical difference between
   * semi-qualified and fully-qualified names.
   */
  @Ensures("result == isBinaryName(name.replace('.', '/'))")
  public static boolean isQualifiedName(String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }
    String[] parts = name.split("\\.");
    for (String part : parts) {
      if (!isSimpleName(part)) {
        return false;
      }
    }
    return true;
  }

  private static final Predicate<String> IS_QUALIFIED_NAME =
      new Predicate<String>() {
    @Override
    public boolean apply(String name) {
      return isQualifiedName(name);
    }
  };

  public static Predicate<String> isQualifiedName() {
    return IS_QUALIFIED_NAME;
  }

  /**
   * Returns {@code true} if {@code name} is <em>syntactically</em> a
   * valid qualified name followed by the two characters {@code .*}.
   */
  public static boolean isStarQualifiedName(String name) {
    return name != null
        && name.endsWith(".*")
        && isQualifiedName(name.substring(0, name.length() - 2));
  }

  /**
   * Returns {@code true} if {@code name} is <em>syntactically</em> a
   * valid binary name.
   */
  @Ensures("result == isQualifiedName(name.replace('/', '.'))")
  public static boolean isBinaryName(String name) {
    if (name == null || name.isEmpty()) {
      return false;
    }
    String[] parts = name.split("/");
    for (String part : parts) {
      if (!isSimpleName(part)) {
        return false;
      }
    }
    return true;
  }

  private static final Predicate<String> IS_BINARY_NAME =
      new Predicate<String>() {
    @Override
    public boolean apply(String name) {
      return isBinaryName(name);
    }
  };

  public static Predicate<String> isBinaryName() {
    return IS_BINARY_NAME;
  }
}
