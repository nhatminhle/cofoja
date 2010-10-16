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
import com.google.java.contract.Requires;
import com.google.java.contract.core.util.JavaUtils;

import java.util.Collections;
import java.util.Iterator;

/**
 * A helper type, used to implement interface contracts.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class HelperTypeModel extends TypeModel {
  /**
   * Constructs a new HelperTypeModel for the interface type
   * {@code original}.
   */
  @Requires({
    "original != null",
    "original.getKind() == ElementKind.INTERFACE"
  })
  public HelperTypeModel(TypeModel original) {
    super(original);

    kind = ElementKind.CLASS;

    String oldQualifiedName = name.getQualifiedName();
    String newQualifiedName = oldQualifiedName + JavaUtils.HELPER_CLASS_SUFFIX;
    String newDeclaredName =
        name.getDeclaredName().replace(oldQualifiedName, newQualifiedName);
    name = new ClassName(name.getBinaryName() + JavaUtils.HELPER_CLASS_SUFFIX,
                         newDeclaredName,
                         name.getSimpleName() + JavaUtils.HELPER_CLASS_SUFFIX);
    simpleName = name.getSimpleName();

    interfaces = Collections.singleton(original.getName());
    superArguments = Collections.emptyList();

    Iterator<ElementModel> iter = enclosedElements.iterator();
    while (iter.hasNext()) {
      if (iter.next().getKind().isType()) {
        iter.remove();
      }
    }
  }

  @Requires("that != null")
  @Ensures("getEnclosingElement() == null")
  public HelperTypeModel(HelperTypeModel that) {
    super(that);
  }

  @Override
  public HelperTypeModel clone() {
    return new HelperTypeModel(this);
  }
}
