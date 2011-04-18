/*
 * Copyright 2007 Johannes Rieken
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
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.TypeModel;
import com.google.java.contract.core.util.JavaUtils;

import java.net.URLClassLoader;
import javax.lang.model.element.TypeElement;

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
class TypeFactory {
  protected URLClassLoader sourceDependencyLoader;

  protected FactoryUtils utils;

  @Requires("utils != null")
  TypeFactory(FactoryUtils utils,
              String sourceDependencyPath) {
    sourceDependencyLoader = null;
    if (sourceDependencyPath != null) {
      sourceDependencyLoader =
          JavaUtils.getLoaderForPath(sourceDependencyPath);
    }

    this.utils = utils;
  }

  /**
   * Returns a {@link TypeModel} instance representing the specified
   * {@link TypeElement}.
   */
  @Requires({
    "element != null",
    "diagnosticManager != null"
  })
  @Ensures({
    "result != null",
    "result.getName().getQualifiedName()" +
        ".equals(element.getQualifiedName().toString())"
  })
  TypeModel createType(TypeElement element,
                       DiagnosticManager diagnosticManager) {
    String name = utils.elementUtils.getBinaryName(element)
        .toString().replace('.', '/');
    TypeBuilder visitor =
        new TypeBuilder(utils, sourceDependencyLoader, diagnosticManager);
    element.accept(visitor, null);
    return visitor.getType();
  }
}
