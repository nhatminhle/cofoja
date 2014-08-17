/*
 * Copyright 2007 Johannes Rieken
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

import com.google.java.contract.ContractImport;
import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ClassName;
import com.google.java.contract.core.model.ContractKind;
import com.google.java.contract.core.model.ContractMethodModel;
import com.google.java.contract.core.model.ElementKind;
import com.google.java.contract.core.model.ElementModifier;
import com.google.java.contract.core.model.MethodModel;
import com.google.java.contract.core.model.TypeModel;
import com.google.java.contract.core.model.TypeName;
import com.google.java.contract.core.model.VariableModel;
import com.google.java.contract.core.util.ElementScanner;
import com.google.java.contract.core.util.Elements;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An element visitor that writes the contract Java source associated
 * with a given {@link Type} as Java source code to an output stream.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author chatain@google.com (Leonardo Chatain)
 */
@ContractImport({
  "com.google.java.contract.core.model.ElementKind",
  "com.google.java.contract.util.Iterables",
  "com.google.java.contract.util.Predicates"
})
@Invariant({
  "getLineNumberMap() != null",
  "Iterables.all(getLineNumberMap().keySet(), Predicates.between(1L, null))",
  "output != null",
  "lineNumber >= 1"
})
public class ContractWriter extends ElementScanner {
  private static final List<String> numericTypes =
      Arrays.asList("char", "byte", "short", "int", "long", "float", "double");

  private static final String CONTRACT_METHOD_SIGNATURE =
      "com.google.java.contract.core.agent.ContractMethodSignature";
  private static final String CONTRACT_KIND =
      "com.google.java.contract.core.model.ContractKind";

  private static final Pattern VARIADIC_REGEX =
      Pattern.compile("\\[\\p{javaWhitespace}*\\]\\p{javaWhitespace}*$");

  protected boolean debugTrace;

  protected ByteArrayOutputStream output;

  protected long lineNumber;

  /**
   * The resulting mapping between contract annotations and generated
   * line numbers.
   */
  protected Map<Long, Object> lineNumberMap;

  /**
   * {@code true} if this visitor is currently visiting the root
   * (top-level) class definition.
   */
  protected boolean isRootClass;

  protected TypeModel type;

  protected ContractWriter() {
    this(false);
  }

  protected ContractWriter(boolean debugTrace) {
    this.debugTrace = debugTrace;
    output = new ByteArrayOutputStream();
    lineNumber = 1;
    lineNumberMap = new HashMap<Long, Object>();
    isRootClass = true;
    type = null;
  }

  @Requires("parent != null")
  protected ContractWriter(ContractWriter parent) {
    debugTrace = parent.debugTrace;
    output = parent.output;
    lineNumber = parent.lineNumber;
    lineNumberMap = parent.lineNumberMap;
    isRootClass = false;
    type = null;
  }

  /**
   * Returns a default value string of the specified type.
   */
  @Requires("type != null")
  @Ensures("result != null")
  protected static String getDefaultValue(TypeName type) {
    String name = type.getDeclaredName();
    if (name.equals("boolean")) {
      return "false";
    } else if (numericTypes.contains(name)) {
      return "(" + name + ")0";
    } else {
      return "(" + name + ")null";
    }
  }

  @Requires("str != null")
  protected void append(String str) {
    try {
      output.write(str.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Ensures("lineNumber == old(lineNumber) + 1")
  protected void appendEndOfLine() {
    output.write('\n');
    ++lineNumber;
  }

  @Requires("info != null")
  @Ensures("lineNumber == old(lineNumber) + 1")
  protected void appendEndOfLine(Object info) {
    lineNumberMap.put(lineNumber, info);
    appendEndOfLine();
  }

  @Requires({
    "list != null",
    "separator != null"
  })
  private void appendJoin(Collection<?> list, String separator) {
    if (list.isEmpty()) {
      return;
    }

    Iterator<?> it = list.iterator();
    for (;;) {
      append(it.next().toString());
      if (!it.hasNext()) {
        break;
      }
      append(separator);
    }
  }

  @Requires("signature != null")
  private void appendGenericSignature(List<? extends TypeName> signature) {
    if (!signature.isEmpty()) {
      append("<");
      appendJoin(signature, ", ");
      append(">");
    }
  }

  @Requires("modifiers != null")
  private void appendModifiers(EnumSet<ElementModifier> modifiers) {
    List<ElementModifier> list = new ArrayList<ElementModifier>(modifiers);
    Collections.sort(list);
    appendJoin(list, " ");
  }

  @Requires("rootType != null")
  private void appendPackageDeclaration(TypeModel rootType) {
    String packageName =
        ClassName.getPackageName(rootType.getName().getSemiQualifiedName());
    if (!packageName.isEmpty()) {
      append("package ");
      append(packageName);
      append(";");
      appendEndOfLine();
    }
  }

  @Requires({
    "rootType != null",
    "rootType.getEnclosingElement() == null"
  })
  private void appendImportStatements(TypeModel rootType) {
    for (String importName : rootType.getImportNames()) {
      append("import ");
      append(importName);
      append(";");
      appendEndOfLine();
    }
  }

  @Requires("variable != null")
  private void appendVariableDeclaration(VariableModel variable) {
    appendVariableDeclaration(variable, null);
  }

  @Requires("variable != null")
  private void appendVariableDeclaration(VariableModel variable,
                                         String typeNameOverride) {
    appendModifiers(variable.getModifiers());
    append(" ");
    if (typeNameOverride == null) {
      append(variable.getType().getDeclaredName());
    } else {
      append(typeNameOverride);
    }
    append(" ");
    append(variable.getSimpleName());
  }

  @Requires("method != null")
  private void appendMethodDeclaration(MethodModel method) {
    EnumSet<ElementModifier> modifiers = method.getModifiers();
    if (type.getKind().isInterfaceType()) {
      modifiers.remove(ElementModifier.ABSTRACT);
    }
    appendModifiers(modifiers);
    append(" ");
    appendGenericSignature(method.getTypeParameters());

    if (method.isConstructor()) {
      append(" ");
      append(method.getEnclosingElement().getSimpleName());
    } else {
      append(" ");
      append(method.getReturnType().getDeclaredName());
      append(" ");
      append(method.getSimpleName());
    }

    append("(");
    Iterator<? extends VariableModel> it = method.getParameters().iterator();
    if (it.hasNext()) {
      VariableModel param = it.next();
      while (it.hasNext()) {
        appendVariableDeclaration(param);
        append(", ");
        param = it.next();
      }
      if (!method.isVariadic()) {
        appendVariableDeclaration(param);
      } else {
        String variadicTypeName =
            VARIADIC_REGEX.matcher(param.getType().getDeclaredName())
            .replaceFirst("...");
        appendVariableDeclaration(param, variadicTypeName);
      }
    }
    append(")");

    Set<? extends TypeName> exceptions = method.getExceptions();
    if (exceptions.size() != 0) {
      append(" throws ");
      appendJoin(exceptions, ", ");
    }
  }

  @Requires({
    "method != null",
    "method.isConstructor()"
  })
  private void appendConstructorCode(MethodModel method) {
    TypeModel parent = Elements.getTypeOf(method);
    List<? extends TypeName> superArguments = parent.getSuperArguments();
    if (!superArguments.isEmpty()) {
      append("super(");
      Iterator<? extends TypeName> it = superArguments.iterator();
      for (;;) {
        append(getDefaultValue(it.next()));
        if (!it.hasNext()) {
          break;
        }
        append(", ");
      }
      append(");");
    }
  }

  @Requires({
    "method != null",
    "!method.isConstructor()"
  })
  private void appendNormalMethodCode(MethodModel method) {
    TypeName returnType = method.getReturnType();
    if (!returnType.getDeclaredName().equals("void")) {
      append("return ");
      append(getDefaultValue(returnType));
      append(";");
    }
  }

  @Requires("contract != null")
  private void appendContractSignature(ContractMethodModel contract) {
    append("@");
    append(CONTRACT_METHOD_SIGNATURE);
    append("(");

    append("kind = ");
    append(CONTRACT_KIND);
    append(".");
    append(contract.getContractKind().name());

    int id = contract.getId();
    if (id != -1) {
      append(", id = ");
      append(Integer.toString(id));
    }

    MethodModel contracted = contract.getContractedMethod();
    if (contracted != null) {
      append(", target = \"");
      append(contracted.getSimpleName());
      append("\"");
    }

    List<Long> lineNumbers = contract.getLineNumbers();
    if (lineNumbers != null && !lineNumbers.isEmpty()) {
      append(", lines = { ");
      Iterator<Long> it = lineNumbers.iterator();
      for (;;) {
        Long lineNumber = it.next();
        append(Long.toString(lineNumber == null ? -1 : lineNumber));
        if (!it.hasNext()) {
          break;
        }
        append(", ");
      }
      append(" }");
    }

    append(")");
    appendEndOfLine();
  }

  @Override
  public void visitVariable(VariableModel variable) {
    if (variable.getKind() == ElementKind.CONSTANT) {
      /* Handled in visitType(). */
      return;
    }

    appendVariableDeclaration(variable);
    if (variable.getModifiers().contains(ElementModifier.FINAL)) {
      append(" = ");
      String defaultValue = getDefaultValue(variable.getType());
      /*
       * Append dummy check to prevent the compiler from substituting the field
       * for its value on contract-checking methods.
       */
      append("\"dummy\".equals(\"dummy\") ? ");
      append(defaultValue);
      append(":");
      append(defaultValue);
    }
    append(";");
    appendEndOfLine();
  }

  @Override
  public void visitContractMethod(ContractMethodModel contract) {
    appendContractSignature(contract);
    appendMethodDeclaration(contract);
    append(" {");
    appendEndOfLine();

    Object info = contract.getSourceInfo();
    if (debugTrace && contract.getContractKind() == ContractKind.HELPER) {
      append("com.google.java.contract.core.util.DebugUtils.contractInfo(");
      append("\"checking contract: ");
      append(quoteString(((TypeModel) contract.getEnclosingElement())
                         .getName().getQualifiedName()));
      append(".");
      append(quoteString(contract.getSimpleName()));
      if (info instanceof AnnotationSourceInfo) {
        AnnotationSourceInfo sourceInfo = (AnnotationSourceInfo) info;
        append(": ");
        append(quoteString(sourceInfo.getAnnotationValue().toString()));
      }
      append("\");");
      appendEndOfLine();
    }
    append(contract.getCode());
    if (info == null) {
      appendEndOfLine();
    } else {
      appendEndOfLine(info);
    }

    append("}");
    appendEndOfLine();
  }

  @Override
  public void visitMethod(MethodModel method) {
    /* Enum constructors are handled in visitType(). */
    if (type.getKind() == ElementKind.ENUM
        && method.getSimpleName().equals("<init>")) {
      return;
    }

    appendMethodDeclaration(method);
    if (type.getKind().isInterfaceType()
        || method.getModifiers().contains(ElementModifier.ABSTRACT)) {
      append(";");
    } else {
      append(" {");
      appendEndOfLine();
      if (method.isConstructor()) {
        appendConstructorCode(method);
      } else {
        appendNormalMethodCode(method);
      }
      appendEndOfLine();
      append("}");
    }
    appendEndOfLine();
  }

  @Override
  public void visitType(TypeModel type) {
    if (this.type != null) {
      ContractWriter subwriter = new ContractWriter(this);
      subwriter.visitType(type);
      lineNumber = subwriter.lineNumber;
      return;
    }
    this.type = type;

    /* Package. */
    if (isRootClass) {
      appendPackageDeclaration(type);
      appendImportStatements(type);
    }

    /* Type and name. */
    appendTypeDeclaration(type);

    /* Superclass. */
    appendSuperclass(type);

    /* Interfaces. */
    appendInterfaces(type);

    /* Body. */
    append(" {");
    appendEndOfLine();

    if (type.getKind() == ElementKind.ENUM) {
      appendEnumSkeleton(type);
    }

    /* Members. */
    scan(type.getEnclosedElements());

    /* End of type. */
    append("}");
    appendEndOfLine();
  }

  @Requires("kind != null")
  private String getKeywordForType(ElementKind kind) {
    String keyword = null;
    switch (type.getKind()) {
      case CLASS:
        keyword = "class";
        break;
      case ENUM:
        keyword = "enum";
        break;
      case INTERFACE:
        keyword = "interface";
        break;
      case ANNOTATION_TYPE:
        keyword = "@interface";
        break;
    }
    return keyword;
  }

  /**
   * Adds enum constants and a dummy constructor.
   */
  @Requires("type != null")
  private void appendEnumSkeleton(TypeModel type) {
    /* Enum constants. */
    List<? extends VariableModel> constants =
        Elements.filter(type.getEnclosedElements(), VariableModel.class,
                        ElementKind.CONSTANT);
    Iterator<? extends VariableModel> it = constants.iterator();
    if (it.hasNext()) {
      for (;;) {
        append(it.next().getSimpleName());
        if (!it.hasNext()) {
          break;
        }
        append(", ");
      }
    }
    append(";");
    appendEndOfLine();

    /* Enum dummy constructor. */
    append("private ");
    append(type.getSimpleName());
    append("() {");
    appendEndOfLine();
    append("}");
    appendEndOfLine();
  }

  /**
   * Adds the type declaration information, including modifiers, name and
   * generic signature.
   */
  private void appendTypeDeclaration(TypeModel type) {
    String keyword = getKeywordForType(type.getKind());
    if (keyword == null)
      throw new IllegalArgumentException();

    /* Type modifiers. */
    EnumSet<ElementModifier> modifiers = type.getModifiers();
    switch(type.getKind()) {
      case INTERFACE:
        modifiers.remove(ElementModifier.ABSTRACT);
        break;
      case ANNOTATION_TYPE:
        modifiers.remove(ElementModifier.ABSTRACT);
        modifiers.remove(ElementModifier.STATIC);
        break;
    }

    appendModifiers(modifiers);
    append(" ");
    append(keyword);
    append(" ");

    /* Type name. */
    String printName = type.getSimpleName();
    append(printName);

    /* Generic parameters. */
    appendGenericSignature(type.getTypeParameters());
  }

  /**
   * Adds superclass information if needed.
   */
  @Requires("type != null")
  private void appendSuperclass(TypeModel type) {
    if (type.getKind() != ElementKind.ENUM
        && type.getSuperclass() != null) {
      append(" extends ");
      append(type.getSuperclass().getDeclaredName());
    }
  }

  /**
   * Appends information about the interfaces implemented by this type.
   * All annotations implicitly implement
   * {@code java.lang.annotation.Annotation}, but this can't be explicitly
   * declared on the mock.
   */
  @Requires({
    "type != null",
    "type.getKind() != ElementKind.ANNOTATION_TYPE" +
        "|| type.getInterfaces().size() == 1"
  })
  private void appendInterfaces(TypeModel type) {
    final ElementKind kind = type.getKind();
    if (kind != ElementKind.ANNOTATION_TYPE) {
      Set<? extends ClassName> interfaces = type.getInterfaces();
      if (interfaces.size() != 0) {
        if (kind == ElementKind.INTERFACE) {
          append(" extends ");
        } else {
          append(" implements ");
        }
        appendJoin(interfaces, ", ");
      }
    }
  }

  /**
   * Backslash-quotes the specified string for inclusion in source
   * code.
   */
  @Requires("s != null")
  @Ensures("result != null")
  public static String quoteString(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  public Map<Long, ?> getLineNumberMap() {
    return lineNumberMap;
  }

  @Ensures("result != null")
  public byte[] toByteArray() {
    return output.toByteArray();
  }
}
