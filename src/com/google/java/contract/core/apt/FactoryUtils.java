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
import com.google.java.contract.core.model.ElementKind;
import com.google.java.contract.core.model.ElementModifier;
import com.google.java.contract.core.model.QualifiedElementModel;
import com.google.java.contract.core.model.TypeName;

import java.util.Iterator;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Utility methods for dealing with {@link javax.lang.model} and their
 * {@link com.google.java.contract.core.apt} counterparts.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({
  "processingEnv != null",
  "elementUtils != null",
  "typeUtils != null"
})
class FactoryUtils {
  ProcessingEnvironment processingEnv;
  Elements elementUtils;
  Types typeUtils;

  @Requires("processingEnv != null")
  @Ensures({
    "this.processingEnv == processingEnv",
    "elementUtils == processingEnv.getElementUtils()",
    "typeUtils == processingEnv.getTypeUtils()"
  })
  FactoryUtils(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    elementUtils = processingEnv.getElementUtils();
    typeUtils = processingEnv.getTypeUtils();
  }

  void copyModifiers(Element e, QualifiedElementModel model) {
    for (ElementModifier modifier :
         ElementModifier.forModifiers(e.getModifiers())) {
      model.addModifier(modifier);
    }
  }

  /**
   * Creates a {@link ClassName} from a {@link TypeMirror}. The
   * created ClassName bears generic parameters, if any.
   */
  @Requires({
    "type != null",
    "type.getKind() == javax.lang.model.type.TypeKind.DECLARED"
  })
  @Ensures("result == null || result.getDeclaredName().equals(type.toString())")
  ClassName getClassNameForType(TypeMirror type) {
    DeclaredType tmp = (DeclaredType) type;
    TypeElement element = (TypeElement) tmp.asElement();
    String binaryName = elementUtils.getBinaryName(element)
        .toString().replace('.', '/');
    return new ClassName(binaryName, type.toString());
  }

  /**
   * Creates a {@link TypeName} from a {@link TypeMirror}.
   */
  @Requires("type != null")
  @Ensures("result == null || result.getDeclaredName().equals(type.toString())")
  TypeName getTypeNameForType(TypeMirror type) {
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
  TypeName getGenericTypeName(TypeParameterElement element) {
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
   * Gets the contract kind of an annotation given its qualified name.
   * Returns null if the annotation is not a contract annotation.
   *
   * @param annotationName the fully qualified name of the annotation
   * @return the contract type, null if not contracts
   */
  ElementKind getAnnotationKindForName(AnnotationMirror annotation) {
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
      kind = null;
    }

    return kind;
  }

  boolean isContractAnnotation(AnnotationMirror annotation) {
    return getAnnotationKindForName(annotation) != null;
  }
}
