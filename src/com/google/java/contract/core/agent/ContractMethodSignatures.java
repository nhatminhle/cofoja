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
package com.google.java.contract.core.agent;

import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ContractKind;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods to read contract method signature components.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @see com.google.java.contract.core.agent.ContractMethodSignature
 */
class ContractMethodSignatures {
  static final String CONTRACT_METHOD_SIGNATURE_DESC =
      Type.getObjectType("com/google/java/contract/core/agent/ContractMethodSignature")
      .getDescriptor();

  @Requires("contractMethod != null")
  static String getTarget(MethodNode contractMethod) {
    return getMetaData(contractMethod, "target", String.class);
  }

  @Requires("contractMethod != null")
  static ContractKind getKind(MethodNode contractMethod) {
    String[] pair = getMetaData(contractMethod, "kind", String[].class);
    if (pair != null) {
      return Enum.valueOf(ContractKind.class, pair[1]);
    } else if (contractMethod.name.startsWith("access$")) {
      return ContractKind.ACCESS;
    } else if (contractMethod.name.startsWith("lambda$")) {
      return ContractKind.LAMBDA;
    } else {
      return null;
    }
  }

  @Requires("contractMethod != null")
  @Ensures("result >= -1")
  static int getId(MethodNode contractMethod) {
    Integer id = getMetaData(contractMethod, "id", Integer.class);
    return id == null || id < 0 ? -1 : id;
  }

  @Requires("contractMethod != null")
  @Ensures("result == null || isLineNumberList(result)")
  static List<Long> getLineNumbers(MethodNode contractMethod) {
    Object lines = getMetaData(contractMethod, "lines", Object.class);
    return getLineNumbers(lines);
  }

  /**
   * Converts either a {@code long[]} or {@code List<Long>} object as
   * obtained by reading the annotation to a {@code List<Long>} with
   * {@code null} instead of negative values to represent non-existent
   * line information.
   *
   * TODO(lenh): ASM documentation states the returned annotation
   * value should be a list but the library returns an array.
   */
  @SuppressWarnings("unchecked")
  @Ensures({
    "lines == null ? result == null : isLineNumberList(result)"
  })
  static List<Long> getLineNumbers(Object lines) {
    if (lines == null) {
      return null;
    }

    ArrayList<Long> lineNumbers = new ArrayList<Long>();
    if (lines.getClass().isArray()) {
      for (long line : (long[]) lines) {
        lineNumbers.add(line < 1 ? null : line);
      }
    } else {
      for (Long line : (List<Long>) lines) {
        lineNumbers.add(line < 1 ? null : line);
      }
    }

    return lineNumbers;
  }

  /**
   * Returns {@code true} if {@code list} is a proper line number list
   * as used by Contracts for Java in the bytecode instrumenter and
   * associated components.
   */
  static boolean isLineNumberList(List<Long> list) {
    if (list == null) {
      return false;
    }
    for (Long line : list) {
      if (line != null && line < 1) {
        return false;
      }
    }
    return true;
  }

  /**
   * Extracts the method signature field named {@code field}, with the
   * type {@code clazz}, from the annotations of
   * {@code contractMethod}.
   *
   * @param contractMethod the annotated method node
   * @param field the name of the field to retrieve
   * @param clazz the type of the value to return
   * @return the value of the field, or {@code null}
   */
  @Requires({
    "contractMethod != null",
    "field != null",
    "clazz != null"
  })
  @SuppressWarnings("unchecked")
  static <T> T getMetaData(MethodNode contractMethod,
                           String field, Class<T> clazz) {
    List<AnnotationNode> annotations = contractMethod.invisibleAnnotations;
    if (annotations == null) {
      return null;
    }

    for (AnnotationNode annotation : annotations) {
      if (!annotation.desc.equals(CONTRACT_METHOD_SIGNATURE_DESC)) {
        continue;
      }

      if (annotation.values == null) {
        return null;
      }

      Iterator<?> it = annotation.values.iterator();
      while (it.hasNext()) {
        String name = (String) it.next();
        Object value = it.next();
        if (name.equals(field)) {
          return (T) value;
        }
      }
    }

    return null;
  }
}
