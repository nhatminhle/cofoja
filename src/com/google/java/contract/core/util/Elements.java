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
package com.google.java.contract.core.util;

import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ElementKind;
import com.google.java.contract.core.model.ElementModel;
import com.google.java.contract.core.model.MethodModel;
import com.google.java.contract.core.model.TypeModel;
import com.google.java.contract.core.model.VariableModel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.tools.JavaFileObject.Kind;

/**
 * Utility methods to ease working with {@link ElementModel} objects.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
public class Elements {
  /**
   * Adds a copy of the {@code parameters} to {@code method}.
   */
  @Requires({
    "method != null",
    "parameters != null",
    "!parameters.contains(null)"
  })
  public static void copyParameters(MethodModel method,
                                    List<? extends VariableModel> parameters) {
    ArrayList<VariableModel> list = new ArrayList<VariableModel>(parameters);
    for (VariableModel param : list) {
      method.addParameter(new VariableModel(param));
    }
  }

  /**
   * Returns the sublist of all elements of the specified kinds.
   */
  @Requires({
    "elements != null",
    "!elements.contains(null)",
    "clazz != null",
    "kinds != null"
  })
  @SuppressWarnings("unchecked")
  public static <T extends ElementModel> List<? extends T> filter(
      List<? extends ElementModel> elements, Class<T> clazz,
      ElementKind... kinds) {
    ArrayList<T> result = new ArrayList<T>();
    List<ElementKind> list = Arrays.asList(kinds);
    for (ElementModel element : elements) {
      if (list.contains(element.getKind())
          && clazz.isAssignableFrom(element.getClass())) {
        result.add((T) element);
      }
    }
    return Collections.unmodifiableList(result);
  }

  /**
   * Returns the closest enclosing element of the specified kind.
   */
  @Requires({
    "element != null",
    "clazz != null",
    "kinds != null"
  })
  @SuppressWarnings("unchecked")
  public static <T extends ElementModel> T findEnclosingElement(
      ElementModel element, Class<T> clazz, ElementKind... kinds) {
    List<ElementKind> list = Arrays.asList(kinds);
    for (;;) {
      element = element.getEnclosingElement();
      if (element == null) {
        return null;
      }
      if (list.contains(element.getKind())
          && clazz.isAssignableFrom(element.getClass())) {
        return (T) element;
      }
    }
  }

  /**
   * Returns the closest enclosing type of the specified element.
   */
  @Requires("element != null")
  public static TypeModel getTypeOf(ElementModel element) {
    return findEnclosingElement(element, TypeModel.class,
        ElementKind.CLASS, ElementKind.ENUM, ElementKind.INTERFACE);
  }

  /**
   * Returns a Contracts for Java private URI for the specified class name
   * and kind.
   */
  @Requires({
    "name != null",
    "!name.isEmpty()",
    "kind != null"
  })
  public static URI getUriForClass(String name, Kind kind) {
    try {
      return new URI("com.google.java.contract://com.google.java.contract/" + name + kind.extension);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException();
    }
  }
}
