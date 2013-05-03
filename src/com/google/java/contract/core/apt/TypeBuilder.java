/*
 * Copyright 2010 Google Inc.
 * Copyright 2011 Nhat Minh Lê
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
import com.google.java.contract.core.model.ContractAnnotationModel;
import com.google.java.contract.core.model.ElementKind;
import com.google.java.contract.core.model.ElementModel;
import com.google.java.contract.core.model.ElementModifier;
import com.google.java.contract.core.model.MethodModel;
import com.google.java.contract.core.model.TypeModel;
import com.google.java.contract.core.model.VariableModel;
import com.google.java.contract.core.util.JavaUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

/**
 * An element visitor that builds a {@link TypeModel} object. It
 * recursively builds child (nested) types. This visitor takes an
 * element parameter, to which children are added.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author chatain@google.com (Leonardo Chatain)
 */
@Invariant({
  "diagnosticManager != null",
  "methodMap != null"
})
class TypeBuilder extends AbstractTypeBuilder {
  protected class ContractableMethod {
    protected ExecutableElement mirror;
    protected MethodModel element;

    public ContractableMethod(ExecutableElement mirror,
                              MethodModel element) {
      this.mirror = mirror;
      this.element = element;
    }
  }

  /**
   * An element visitor that adds inherited contract annotations to
   * an existing TypeModel object.
   */
  protected class ContractExtensionBuilder extends AbstractTypeBuilder {
    protected TypeElement mirror;

    public ContractExtensionBuilder() {
      super(TypeBuilder.this.utils, TypeBuilder.this.diagnosticManager);
    }

    @Override
    public Void visitType(TypeElement e, ElementModel p) {
      if (mirror != null) {
        throw new IllegalStateException();
      }

      mirror = e;
      scanAnnotations(e, false, utils.getClassNameForType(e.asType()), type);

      scan(ElementFilter.methodsIn(e.getEnclosedElements()), type);

      scanSuper(e);
      return null;
    }

    @Override
    public Void visitExecutable(ExecutableElement e, ElementModel p) {
      String name = e.getSimpleName().toString();
      ArrayList<ContractableMethod> candidates = methodMap.get(name);
      if (candidates == null) {
        return null;
      }
      for (ContractableMethod overrider : candidates) {
        if (utils.elementUtils.overrides(overrider.mirror, e,
                                         rootMirror)) {
          scanAnnotations(e, false,
                          utils.getClassNameForType(mirror.asType()),
                          overrider.element);
        }
      }
      return null;
    }
  }

  protected ClassLoader sourceDependencyLoader;

  /**
   * The resulting top-level type.
   */
  protected TypeModel type;

  /**
   * The root TypeElement.
   */
  protected TypeElement rootMirror;

  /**
   * Import statements that apply to this type source code.
   */
  protected Set<String> importNames;

  /**
   * A map of resulting methods; used internally for contract
   * inheritance propagation.
   */
  protected HashMap<String, ArrayList<ContractableMethod>> methodMap;

  TypeBuilder(Set<String> importNames,
              Iterator<Long> rootLineNumberIterator,
              FactoryUtils utils,
              ClassLoader sourceDependencyLoader,
              DiagnosticManager diagnosticManager) {
    super(utils, diagnosticManager);
    this.sourceDependencyLoader = sourceDependencyLoader;
    type = null;
    rootMirror = null;
    this.importNames = importNames;
    this.rootLineNumberIterator = rootLineNumberIterator;
    methodMap = new HashMap<String, ArrayList<ContractableMethod>>();
  }

  TypeBuilder(FactoryUtils utils,
              ClassLoader sourceDependencyLoader,
              DiagnosticManager diagnosticManager) {
    this(null, null, utils, sourceDependencyLoader, diagnosticManager);
  }

  TypeModel getType() {
    return type;
  }

  @Override
  @Ensures("type != null")
  public Void visitType(TypeElement e, ElementModel p) {
    /* Inner types. */
    if (type != null) {
      TypeBuilder builder =
          new TypeBuilder(importNames, rootLineNumberIterator,
                          utils, sourceDependencyLoader, diagnosticManager);
      e.accept(builder, p);
      p.addEnclosedElement(builder.type);
      return null;
    }

    /* Root type. */
    rootMirror = e;

    /* Create type. */
    ElementKind kind = null;
    switch (e.getKind()) {
      case INTERFACE:
        kind = ElementKind.INTERFACE;
        break;
      case ENUM:
        kind = ElementKind.ENUM;
        break;
      case CLASS:
        kind = ElementKind.CLASS;
        break;
      case ANNOTATION_TYPE:
        kind = ElementKind.ANNOTATION_TYPE;
        break;
      default:
        return null;
    }
    type = new TypeModel(kind, utils.getClassNameForType(e.asType()));
    utils.copyModifiers(e, type);
    if (kind == ElementKind.ENUM) {
      type.removeModifier(ElementModifier.FINAL);
    }

    /*
     * Fetch import and global line number information, from a
     * source dependency file, if available, falling back to
     * com.sun.source, if not.
     */
    if (importNames == null) {
      if (sourceDependencyLoader != null) {
        try {
          fetchSourceDependency();
        } catch (IOException ioe) {
          /* Consider that no information is available. */
        }
      }
      if (importNames == null) {
        importNames = getImportNames(e);
      }
      for (String importName : importNames) {
        type.addImportName(importName);
      }
    }

    /* Set superclass and interfaces. */
    TypeMirror superclass = e.getSuperclass();
    if (superclass.getKind() == TypeKind.DECLARED) {
      type.setSuperclass(utils.getClassNameForType(superclass));
    }
    List<? extends TypeMirror> interfaces = e.getInterfaces();
    for (TypeMirror iface : interfaces) {
      type.addInterface(utils.getClassNameForType(iface));
    }

    /* Construct super() call. */
    if (kind != ElementKind.ENUM) {
      TypeMirror superMirror = e.getSuperclass();
      if (superMirror.getKind() == TypeKind.DECLARED) {
        TypeElement superType =
            (TypeElement) utils.typeUtils.asElement(superMirror);
        SuperCallBuilder visitor =
            new SuperCallBuilder((DeclaredType) superMirror, type, utils);
        superType.accept(visitor, null);
      }
    }

    /* Process generic signature. */
    List<? extends TypeParameterElement> typeParams = e.getTypeParameters();
    for (TypeParameterElement tp : typeParams) {
      type.addTypeParameter(utils.getGenericTypeName(tp));
    }

    /* Process annotations. */
    scanAnnotations(e, true, type.getName(), type);

    /* Process members. */
    scan(e.getEnclosedElements(), type);

    /* Add inherited contract annotations. */
    scanSuper(e);

    return null;
  }

  @Override
  protected void visitAnnotation(Element parent, AnnotationMirror annotation,
                                 boolean primary, ClassName owner,
                                 ElementModel p) {
    if (utils.isContractAnnotation(annotation)) {
      ContractAnnotationModel model =
          createContractModel(parent, annotation, primary, owner);
      if (model == null) {
        return;
      }
      if (type.getKind() == ElementKind.ANNOTATION_TYPE) {
        AnnotationSourceInfo asi = (AnnotationSourceInfo) model.getSourceInfo();
        /* Do not add contracts to annotations. Warn the user instead. */
        diagnosticManager.warning("Contracts can't be applied to annotations. "
                                  + "The following annotation will not "
                                  + "perform any contract check: " +
                                  type.toString(),
                                  asi.getAnnotationValue().toString(), 0, 0, 0,
                                  asi);
      } else {
        p.addEnclosedElement(model);
      }
    }
  }

  /**
   * Fetches source dependency information from the system class
   * loader.
   */
  @Requires({
    "sourceDependencyLoader != null",
    "type != null"
  })
  @Ensures({
    "importNames != null",
    "rootLineNumberIterator != null"
  })
  @SuppressWarnings("unchecked")
  protected void fetchSourceDependency() throws IOException {
    String fileName = type.getName().getBinaryName()
        + JavaUtils.SOURCE_DEPENDENCY_EXTENSION;
    InputStream in =
        sourceDependencyLoader.getResourceAsStream(fileName);
    if (in == null) {
      throw new FileNotFoundException();
    }
    ObjectInputStream oin = new ObjectInputStream(in);
    try {
      importNames = (Set<String>) oin.readObject();
      rootLineNumberIterator = ((List<Long>) oin.readObject()).iterator();
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
    }
    oin.close();
  }

  @Override
  public Void visitVariable(VariableElement e, ElementModel p) {
    ElementKind kind = null;
    switch (e.getKind()) {
      case ENUM_CONSTANT:
        kind = ElementKind.CONSTANT;
        break;
      default:
        if (p.getKind().isType()) {
          kind = ElementKind.FIELD;
        } else {
          kind = ElementKind.PARAMETER;
        }
    }
    VariableModel variable =
        new VariableModel(kind, e.getSimpleName().toString(),
                          utils.getTypeNameForType(e.asType()));
    utils.copyModifiers(e, variable);

    scanAnnotations(e, true, type.getName(), variable);

    p.addEnclosedElement(variable);
    return null;
  }

  @Override
  public Void visitExecutable(ExecutableElement e, ElementModel p) {
    MethodModel exec = null;
    String name = e.getSimpleName().toString();

    /*
     * For enum types, synthesized methods values() and
     * valueOf(String) are reflected by the API but must not be
     * reproduced in the mock.
     */
    if (p.getKind() == ElementKind.ENUM) {
      ExecutableType t = (ExecutableType) e.asType();
      if (name.equals("values")) {
        if (t.getParameterTypes().isEmpty()) {
          return null;
        }
      } else if (name.equals("valueOf")) {
        List<TypeMirror> valueOfParameterTypes =
            Collections.singletonList(
                utils.elementUtils
                .getTypeElement("java.lang.String").asType());
        if (t.getParameterTypes().equals(valueOfParameterTypes)) {
          return null;
        }
      }
    }

    /* Create element; decide if constructor or not. */
    if (name.toString().equals("<init>")) {
      exec = new MethodModel();
    } else {
      exec = new MethodModel(ElementKind.METHOD, name,
                             utils.getTypeNameForType(e.getReturnType()));
    }
    utils.copyModifiers(e, exec);

    /* Add generic signature. */
    List<? extends TypeParameterElement> genericTypes = e.getTypeParameters();
    for (TypeParameterElement tp : genericTypes) {
      exec.addTypeParameter(utils.getGenericTypeName(tp));
    }

    /* Add parameters. */
    scan(e.getParameters(), exec);
    exec.setVariadic(e.isVarArgs());

    /* Add throws list. */
    for (TypeMirror tt : e.getThrownTypes()) {
      exec.addException(utils.getTypeNameForType(tt));
    }

    /* Add annotations. */
    scanAnnotations(e, true, type.getName(), exec);

    /* Register method. */
    p.addEnclosedElement(exec);
    addMethod(name, e, exec);

    return null;
  }

  /**
   * Adds an association to the method map.
   */
  protected void addMethod(String k, ExecutableElement e, MethodModel exec) {
    ArrayList<ContractableMethod> list = methodMap.get(k);
    if (list == null) {
      list = new ArrayList<ContractableMethod>();
      methodMap.put(k, list);
    }
    list.add(new ContractableMethod(e, exec));
  }

  /**
   * Visits the superclass and interfaces of the specified
   * TypeElement with a ContractExtensionBuilder.
   */
  protected void scanSuper(TypeElement e) {
    TypeElement superElement =
        (TypeElement) utils.typeUtils.asElement(e.getSuperclass());
    if (superElement != null) {
      superElement.accept(new ContractExtensionBuilder(), type);
    }
    for (TypeMirror iface : e.getInterfaces()) {
      TypeElement ifaceElement =
          (TypeElement) utils.typeUtils.asElement(iface);
      ifaceElement.accept(new ContractExtensionBuilder(), type);
    }
  }
}
