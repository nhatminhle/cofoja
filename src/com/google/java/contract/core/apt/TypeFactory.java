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

import com.google.java.contract.Contracted;
import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ClassName;
import com.google.java.contract.core.model.ContractAnnotationModel;
import com.google.java.contract.core.model.ElementKind;
import com.google.java.contract.core.model.ElementModel;
import com.google.java.contract.core.model.ElementModifier;
import com.google.java.contract.core.model.MethodModel;
import com.google.java.contract.core.model.QualifiedElementModel;
import com.google.java.contract.core.model.TypeModel;
import com.google.java.contract.core.model.TypeName;
import com.google.java.contract.core.model.VariableModel;
import com.google.java.contract.core.util.JavaUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
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
import javax.lang.model.util.ElementScanner6;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * The TypeFactory creates {@link Type} objects from
 * {@link TypeElement} objects. {@link Type} and all nested elements
 * reflect the structure of Java types (classes, interfaces, methods,
 * fields, annotations, ...) in Contracts for Java. Only the needed
 * parts are reflected; unnecessary information is discarded.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
@Invariant({
  "processingEnv != null",
  "elementUtils != null",
  "typeUtils != null"
})
class TypeFactory {
  /**
   * An element visitor that extracts constructor arguments from a
   * callable constructor. It always picks a constructor with the
   * broadest access level: if the original class was correct, then at
   * least that one should be accessible.
   */
  @Invariant("subtype != null")
  protected class SuperCallBuilder extends ElementScanner6<Void, Void> {
    protected DeclaredType typeMirror;
    protected TypeModel subtype;
    protected ElementModifier constructorFound;

    @Requires("subtype != null")
    public SuperCallBuilder(DeclaredType typeMirror, TypeModel subtype) {
      this.typeMirror = typeMirror;
      this.subtype = subtype;
      constructorFound = null;
    }

    @Override
    public Void visitType(TypeElement e, Void unused) {
      scan(ElementFilter.constructorsIn(e.getEnclosedElements()), null);
      return null;
    }

    @Override
    public Void visitExecutable(ExecutableElement e, Void unused) {
      ElementModifier v = ElementModifier.visibilityIn(
          ElementModifier.forModifiers(e.getModifiers()));
      if (constructorFound != null
          && constructorFound.ordinal() <= v.ordinal()) {
        return null;
      }

      subtype.clearSuperArguments();
      ExecutableType execType =
          (ExecutableType) typeUtils.asMemberOf(typeMirror, e);
      List<? extends TypeMirror> paramTypes = execType.getParameterTypes();
      for (TypeMirror t : paramTypes) {
        subtype.addSuperArgument(getTypeNameForType(t));
      }

      constructorFound = v;
      return null;
    }
  }

  /**
   * Abstract base class providing annotation processing facilities
   * used to build types.
   */
  protected class AbstractTypeBuilder
      extends ElementScanner6<Void, ElementModel> {
    protected boolean primary;
    protected boolean virtual;
    protected ClassName owner;

    /**
     * A global iterator to use to get contract annotation line number
     * information.
     */
    protected Iterator<Long> rootLineNumberIterator;

    /**
     * Visits an annotation and adds a corresponding node to the
     * specified Element.
     *
     * Despite the name, this method is not inherited through any
     * visitor interface. It is not intended for external calls.
     *
     * @param parent the target of the annotation
     * @param annotation the annotation
     * @param primary whether this is a primary contract annotation
     * @param virtual whether this is a virtual contract annotation
     * @param owner the owner of this annotation
     * @param p the element to add the created annotation to
     *
     * @see ContractAnnotationModel
     */
    @Requires({
      "parent != null",
      "annotation != null",
      "owner != null",
      "p != null"
    })
    protected void visitAnnotation(
        Element parent, AnnotationMirror annotation,
        boolean primary, boolean virtual, ClassName owner,
        ElementModel p) {
      String annotationName = annotation.getAnnotationType().toString();

      ElementKind kind;
      if (annotationName.equals("com.google.java.contract.Invariant")) {
        kind = ElementKind.INVARIANT;
      } else if (annotationName.equals("com.google.java.contract.Requires")) {
        kind = ElementKind.REQUIRES;
      } else if (annotationName.equals("com.google.java.contract.Ensures")) {
        kind = ElementKind.ENSURES;
      } else if (annotationName.equals("com.google.java.contract.ThrowEnsures")) {
        kind = ElementKind.THROW_ENSURES;
      } else {
        return;
      }

      TypeName returnType = null;
      if (parent.getKind() == javax.lang.model.element.ElementKind.METHOD) {
        ExecutableElement method = (ExecutableElement) parent;
        returnType = getTypeNameForType(
            typeUtils.erasure(method.getReturnType()));
      }
      ContractAnnotationModel model =
          new ContractAnnotationModel(kind, primary, virtual,
                                      owner, returnType);
      List<Long> lineNumbers = null;
      if (rootLineNumberIterator == null) {
        lineNumbers = getLineNumbers(parent, annotation);
      }

      for (AnnotationValue annotationValue :
           annotation.getElementValues().values()) {
        @SuppressWarnings("unchecked")
        List<? extends AnnotationValue> values =
            (List<? extends AnnotationValue>) annotationValue.getValue();

        Iterator<? extends AnnotationValue> iterValue = values.iterator();
        Iterator<Long> iterLineNumber;
        if (rootLineNumberIterator != null) {
          iterLineNumber = rootLineNumberIterator;
        } else {
          iterLineNumber = lineNumbers.iterator();
        }
        while (iterValue.hasNext()) {
          String value = (String) iterValue.next().getValue();
          Long lineNumber =
              iterLineNumber.hasNext() ? iterLineNumber.next() : null;
          model.addValue(value, lineNumber);
        }

        AnnotationSourceInfo sourceInfo =
            new AnnotationSourceInfo(parent, annotation, annotationValue,
                                     model.getValues());
        model.setSourceInfo(sourceInfo);
      }

      p.addEnclosedElement(model);
    }

    /**
     * Returns the line numbers associated with {@code annotation} if
     * available.
     */
    @Requires({
      "parent != null",
      "annotation != null"
    })
    @Ensures("result != null")
    @SuppressWarnings("unchecked")
    protected List<Long> getLineNumbers(Element parent,
                                        AnnotationMirror annotation) {
      if (JavaUtils.classExists("com.sun.source.util.Trees")) {
        try {
          return (List<Long>) Class
              .forName("com.google.java.contract.core.apt.JavacUtils")
              .getMethod("getLineNumbers", ProcessingEnvironment.class,
                         Element.class, AnnotationMirror.class)
              .invoke(null, processingEnv, parent, annotation);
        } catch (Exception e) {
          return Collections.emptyList();
        }
      } else {
        return Collections.emptyList();
      }
    }

    /**
     * Returns the import statements in effect in the compilation unit
     * containing {@code element}.
     */
    @Requires("element != null")
    @Ensures("result != null")
    @SuppressWarnings("unchecked")
    protected Set<String> getImportNames(Element element) {
      if (JavaUtils.classExists("com.sun.source.util.Trees")) {
        try {
          return (Set<String>) Class
              .forName("com.google.java.contract.core.apt.JavacUtils")
              .getMethod("getImportNames", ProcessingEnvironment.class,
                         Element.class)
              .invoke(null, processingEnv, element);
        } catch (Exception e) {
          return Collections.emptySet();
        }
      } else {
        return Collections.emptySet();
      }
    }

    /**
     * Scans a list of annotations and call
     * {@link #visitAnnotation(Element, AnnotationMirror,boolean,boolean,ClassName,ElementModel)}
     * on each one of them, in order.
     *
     * @see ContractAnnotationModel
     */
    @Requires({
      "parent != null",
      "annotations != null",
      "owner != null",
      "p != null"
    })
    protected void scanAnnotations(Element parent,
        List<? extends AnnotationMirror> annotations,
        boolean primary, boolean virtual, ClassName owner,
        ElementModel p) {
      for (AnnotationMirror ann : annotations) {
        visitAnnotation(parent, ann, primary, virtual, owner, p);
      }
    }
  }

  /**
   * An element visitor that builds a TypeModel object. It recursively
   * builds child (nested) types. This visitor takes an element
   * parameter, to which children are added.
   */
  @Invariant("methodMap != null")
  protected class TypeBuilder extends AbstractTypeBuilder {
    protected class ContractableMethod {
      protected ExecutableElement mirror;
      protected MethodModel element;

      private ContractableMethod(ExecutableElement mirror,
                                 MethodModel element) {
        this.mirror = mirror;
        this.element = element;
      }
    }

    /**
     * An element visitor that adds inherited contract annotations to
     * an existing TypeModel object.
     */
    @Contracted
    protected class ContractExtensionBuilder extends AbstractTypeBuilder {
      protected TypeElement mirror;
      protected boolean isInterface;

      @Override
      public Void visitType(TypeElement e, ElementModel p) {
        if (mirror != null) {
          throw new IllegalStateException();
        }

        mirror = e;
        isInterface =
            e.getKind() == javax.lang.model.element.ElementKind.INTERFACE;
        scanAnnotations(e, e.getAnnotationMirrors(),
                        false, !isInterface,
                        getClassNameForType(e.asType()), type);

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
          if (elementUtils.overrides(overrider.mirror, e, rootMirror)) {
            scanAnnotations(e, e.getAnnotationMirrors(),
                            false, !isInterface,
                            getClassNameForType(mirror.asType()),
                            overrider.element);
          }
        }
        return null;
      }
    }

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

    public TypeBuilder(Set<String> importNames,
                       Iterator<Long> rootLineNumberIterator) {
      type = null;
      rootMirror = null;
      this.importNames = importNames;
      this.rootLineNumberIterator = rootLineNumberIterator;
      methodMap = new HashMap<String, ArrayList<ContractableMethod>>();
    }

    public TypeBuilder() {
      this(null, null);
    }

    public TypeModel getType() {
      return type;
    }

    @Override
    @Ensures("type != null")
    public Void visitType(TypeElement e, ElementModel p) {
      /* Inner types. */
      if (type != null) {
        TypeBuilder builder =
            new TypeBuilder(importNames, rootLineNumberIterator);
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
        default:
          return null;
      }
      type = new TypeModel(kind, getClassNameForType(e.asType()));
      copyModifiers(e, type);
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
        type.setSuperclass(getClassNameForType(superclass));
      }
      List<? extends TypeMirror> interfaces = e.getInterfaces();
      for (TypeMirror iface : interfaces) {
        type.addInterface(getClassNameForType(iface));
      }

      /* Construct super() call. */
      if (kind != ElementKind.ENUM) {
        TypeMirror superMirror = e.getSuperclass();
        if (superMirror.getKind() == TypeKind.DECLARED) {
          TypeElement superType =
              (TypeElement) typeUtils.asElement(superMirror);
          SuperCallBuilder visitor =
              new SuperCallBuilder((DeclaredType) superMirror, type);
          superType.accept(visitor, null);
        }
      }

      /* Process generic signature. */
      List<? extends TypeParameterElement> typeParams = e.getTypeParameters();
      for (TypeParameterElement tp : typeParams) {
        type.addTypeParameter(getGenericTypeName(tp));
      }

      /* Process members. */
      scanAnnotations(e, e.getAnnotationMirrors(),
                      true, type.getKind() != ElementKind.INTERFACE,
                      type.getName(), type);
      scan(e.getEnclosedElements(), type);

      /* Add inherited contract annotations. */
      scanSuper(e);

      return null;
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
                            getTypeNameForType(e.asType()));
      copyModifiers(e, variable);

      scanAnnotations(e, e.getAnnotationMirrors(),
                      true, type.getKind() != ElementKind.INTERFACE,
                      type.getName(), variable);

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
                  elementUtils.getTypeElement("java.lang.String").asType());
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
                               getTypeNameForType(e.getReturnType()));
      }
      copyModifiers(e, exec);

      /* Add generic signature. */
      List<? extends TypeParameterElement> genericTypes = e.getTypeParameters();
      for (TypeParameterElement tp : genericTypes) {
        exec.addTypeParameter(getGenericTypeName(tp));
      }

      /* Add parameters. */
      scan(e.getParameters(), exec);

      /* Add throws list. */
      for (TypeMirror tt : e.getThrownTypes()) {
        exec.addException(getTypeNameForType(tt));
      }

      /* Add annotations. */
      scanAnnotations(e, e.getAnnotationMirrors(),
                      true, type.getKind() != ElementKind.INTERFACE,
                      type.getName(), exec);

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
          (TypeElement) typeUtils.asElement(e.getSuperclass());
      if (superElement != null) {
        superElement.accept(new ContractExtensionBuilder(), type);
      }
      for (TypeMirror iface : e.getInterfaces()) {
        TypeElement ifaceElement = (TypeElement) typeUtils.asElement(iface);
        ifaceElement.accept(new ContractExtensionBuilder(), type);
      }
    }
  }

  protected URLClassLoader sourceDependencyLoader;

  protected ProcessingEnvironment processingEnv;
  protected Elements elementUtils;
  protected Types typeUtils;

  @Requires("processingEnv != null")
  TypeFactory(ProcessingEnvironment processingEnv,
              String sourceDependencyPath) {
    sourceDependencyLoader = null;
    if (sourceDependencyPath != null) {
      String[] parts =
          sourceDependencyPath.split(Pattern.quote(File.pathSeparator));
      URL[] urls = new URL[parts.length];
      for (int i = 0; i < parts.length; ++i) {
        try {
          urls[i] = new File(parts[i]).toURI().toURL();
        } catch (MalformedURLException e) {
          /* Ignore erroneous paths. */
        }
      }
      sourceDependencyLoader = new URLClassLoader(urls);
    }

    this.processingEnv = processingEnv;
    this.elementUtils = processingEnv.getElementUtils();
    this.typeUtils = processingEnv.getTypeUtils();
  }

  private static void copyModifiers(Element e, QualifiedElementModel model) {
    for (ElementModifier modifier :
         ElementModifier.forModifiers(e.getModifiers())) {
      model.addModifier(modifier);
    }
  }

  /**
   * Creates a ClassName from a TypeMirror. The created ClassName
   * bears generic parameters, if any.
   */
  @Requires({
    "type != null",
    "type.getKind() == javax.lang.model.type.TypeKind.DECLARED"
  })
  @Ensures("result == null || result.getDeclaredName().equals(type.toString())")
  protected ClassName getClassNameForType(TypeMirror type) {
    DeclaredType tmp = (DeclaredType) type;
    TypeElement element = (TypeElement) tmp.asElement();
    String binaryName = elementUtils.getBinaryName(element)
        .toString().replace('.', '/');
    return new ClassName(binaryName, type.toString());
  }

  @Requires("type != null")
  @Ensures("result == null || result.getDeclaredName().equals(type.toString())")
  protected TypeName getTypeNameForType(TypeMirror type) {
    switch (type.getKind()) {
      case NONE:
        return null;
      default:
        return new TypeName(type.toString());
    }
  }

  /**
   * Returns a Java-printable generic type name from the specified
   * TypeParameterElement.
   */
  @Requires("element != null")
  @Ensures("result != null")
  protected static TypeName getGenericTypeName(TypeParameterElement element) {
    String name = element.getSimpleName().toString();
    List<? extends TypeMirror> bounds = element.getBounds();
    if (bounds.isEmpty()
        || (bounds.size() == 1
            && bounds.get(0).toString().equals("java.lang.Object"))) {
      return new TypeName(name);
    }

    StringBuilder buffer = new StringBuilder();
    buffer.append(name);
    buffer.append(" extends ");

    Iterator<? extends TypeMirror> iter = bounds.iterator();
    for (;;) {
      buffer.append(iter.next().toString());
      if (!iter.hasNext()) {
        break;
      }
      buffer.append(" & ");
    }

    return new TypeName(buffer.toString());
  }

  /**
   * Returns a {@link TypeModel} instanec representing the specified
   * {@link TypeElement}.
   */
  @Requires("element != null")
  @Ensures({
    "result != null",
    "result.getName().getQualifiedName()" +
        ".equals(element.getQualifiedName().toString())"
  })
  TypeModel createType(TypeElement element) {
    String name = elementUtils.getBinaryName(element)
        .toString().replace('.', '/');
    TypeBuilder visitor = new TypeBuilder();
    element.accept(visitor, null);
    return visitor.getType();
  }
}
