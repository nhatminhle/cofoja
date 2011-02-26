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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
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

    utils = new FactoryUtils(processingEnv);
  }

  /**
   * Returns a {@link TypeModel} instanec representing the specified
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
