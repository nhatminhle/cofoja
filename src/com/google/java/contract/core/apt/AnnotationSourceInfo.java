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

import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;

/**
 * Source-tracking information, for error reporting.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({
  "element != null",
  "annotationMirror != null",
  "annotationValue != null",
  "code != null",
  "!code.isEmpty()",
  "!code.contains(null)"
})
public class AnnotationSourceInfo {
  protected Element element;
  protected AnnotationMirror annotationMirror;
  protected AnnotationValue annotationValue;
  protected List<String> code;

  @Requires({
    "element != null",
    "annotationMirror != null",
    "annotationValue != null",
    "code != null",
    "!code.isEmpty()"
  })
  @Ensures({
    "element == getElement()",
    "annotationMirror == getAnnotationMirror()",
    "annotationValue == getAnnotationValue()",
    "code.equals(getCode())"
  })
  public AnnotationSourceInfo(Element element,
      AnnotationMirror annotationMirror, AnnotationValue annotationValue,
      List<String> code) {
    this.element = element;
    this.annotationMirror = annotationMirror;
    this.annotationValue = annotationValue;
    this.code = new ArrayList<String>(code);
  }

  @Ensures("result != null")
  public AnnotationMirror getAnnotationMirror() {
    return annotationMirror;
  }

  @Ensures("result != null")
  public AnnotationValue getAnnotationValue() {
    return annotationValue;
  }

  @Ensures({
    "result != null",
    "!result.isEmpty()"
  })
  public List<String> getCode() {
    return code;
  }

  @Ensures("result != null")
  public Element getElement() {
    return element;
  }
}
