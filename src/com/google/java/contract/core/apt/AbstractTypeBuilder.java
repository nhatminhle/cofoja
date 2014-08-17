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
import com.google.java.contract.core.model.TypeName;
import com.google.java.contract.core.util.JavaUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementScanner6;

/**
 * Abstract base class providing annotation processing facilities
 * used to build types.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author chatain@google.com (Leonardo Chatain)
 */
@Invariant("utils != null")
abstract class AbstractTypeBuilder
    extends ElementScanner6<Void, ElementModel> {
  protected FactoryUtils utils;

  protected DiagnosticManager diagnosticManager;

  @Requires("utils != null")
  protected AbstractTypeBuilder(FactoryUtils utils,
                                DiagnosticManager diagnosticManager) {
    this.utils = utils;
    this.diagnosticManager = diagnosticManager;
  }

  /**
   * A global iterator to use to get contract annotation line number
   * information.
   */
  protected Iterator<Long> rootLineNumberIterator;

  /**
   * Creates a blank {@code ContractAnnotationModel} from
   * an {@code AnnotationMirror}. The returned model is created with
   * the correct properties for the provided context but does not
   * contain any of the clauses from {@code annotation}.
   *
   * @param parent the target of the annotation
   * @param annotation the annotation
   * @param primary whether this is a primary contract annotation
   * @param owner the owner of this annotation
   * @return the contract model of this annotation
   */
  @Requires({
    "parent != null",
    "annotation != null",
    "owner != null",
    "utils.isContractAnnotation(annotation)"
  })
  @Ensures("result != null")
  private ContractAnnotationModel createBlankContractModel(Element parent,
      AnnotationMirror annotation, boolean primary, ClassName owner) {
    ElementKind kind = utils.getAnnotationKindForName(annotation);

    boolean virtual;
    TypeName returnType;
    switch (parent.getKind()) {
      default:
        virtual =
            parent.getKind()
            != javax.lang.model.element.ElementKind.INTERFACE;
        returnType = null;
        break;
      case CONSTRUCTOR:
      case METHOD:
        virtual =
            parent.getEnclosingElement().getKind()
            != javax.lang.model.element.ElementKind.INTERFACE;
        ExecutableElement method = (ExecutableElement) parent;
        switch (method.getReturnType().getKind()) {
          case VOID:
            /* For void methods. */
            returnType = utils.getTypeNameForType(method.getReturnType());
            break;
          case NONE:
            /* For constructors. */
            returnType = null;
            break;
          case PACKAGE:
            /* Should not happen. */
            throw new RuntimeException(
                "ExecutableElement has PACKAGE return type");
          default:
            returnType = utils.getTypeNameForType(
                utils.typeUtils.erasure(method.getReturnType()));
        }
    }

    return new ContractAnnotationModel(kind, primary, virtual,
                                       owner, returnType);
  }

  /**
   * Creates a {@code ContractAnnotationModel} from
   * an {@code AnnotationMirror}.
   *
   * @param parent the target of the annotation
   * @param annotation the annotation
   * @param primary whether this is a primary contract annotation
   * @param owner the owner of this annotation
   * @return the contract model of this annotation, or {@code null} if
   * the annotation contains no contract (no or empty value)
   */
  @Requires({
    "parent != null",
    "annotation != null",
    "owner != null",
    "utils.isContractAnnotation(annotation)"
  })
  protected ContractAnnotationModel createContractModel(Element parent,
      AnnotationMirror annotation, boolean primary, ClassName owner) {
    ContractAnnotationModel model = createBlankContractModel(
        parent, annotation, primary, owner);
    List<Long> lineNumbers = null;
    if (rootLineNumberIterator == null) {
      lineNumbers = getLineNumbers(parent, annotation);
    }

    AnnotationValue lastAnnotationValue = null;
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
      lastAnnotationValue = annotationValue;
    }
    if (model.getValues().isEmpty()) {
      diagnosticManager.warning("No contracts specified in annotation.",
                                null, 0, 0, 0,
                                parent, annotation, lastAnnotationValue);
      return null;
    }
    AnnotationSourceInfo sourceInfo =
        new AnnotationSourceInfo(parent, annotation, lastAnnotationValue,
                                 model.getValues());
    model.setSourceInfo(sourceInfo);
    return model;
  }

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
      boolean primary, ClassName owner, ElementModel p) {
    if (utils.isContractAnnotation(annotation)) {
      ContractAnnotationModel model =
          createContractModel(parent, annotation, primary, owner);
      if (model != null) {
        p.addEnclosedElement(model);
      }
    }
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
            .invoke(null, utils.processingEnv, parent, annotation);
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
    HashSet<String> importNames = new HashSet<String>();
    if (JavaUtils.classExists("com.sun.source.util.Trees")) {
      try {
        Set<String> classImportNames = (Set<String>) Class
            .forName("com.google.java.contract.core.apt.JavacUtils")
            .getMethod("getImportNames", ProcessingEnvironment.class,
                       Element.class)
            .invoke(null, utils.processingEnv, element);
        importNames.addAll(classImportNames);
      } catch (Exception e) {
        /* Ignore. */
      }
    }

    /* Add import statements from explicit annotations. */
    for (AnnotationMirror ann : element.getAnnotationMirrors()) {
      if (!ann.getAnnotationType().toString()
          .equals("com.google.java.contract.ContractImport")) {
        continue;
      }
      for (AnnotationValue annotationValue :
           ann.getElementValues().values()) {
        @SuppressWarnings("unchecked")
        List<? extends AnnotationValue> values =
            (List<? extends AnnotationValue>) annotationValue.getValue();
        for (AnnotationValue value : values)
        importNames.add((String) value.getValue());
      }
    }

    return importNames;
  }

  /**
   * Scans a list of annotations and call
   * {@link #visitAnnotation(Element,AnnotationMirror,boolean,ClassName,ElementModel)}
   * on each one of them, in order.
   *
   * @see ContractAnnotationModel
   */
  @Requires({
    "parent != null",
    "owner != null",
    "p != null"
  })
  protected void scanAnnotations(Element parent,
      boolean primary, ClassName owner, ElementModel p) {
    for (AnnotationMirror ann : parent.getAnnotationMirrors()) {
      visitAnnotation(parent, ann, primary, owner, p);
    }
  }
}
