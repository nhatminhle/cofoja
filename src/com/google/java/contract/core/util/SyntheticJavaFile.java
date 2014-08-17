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

import com.google.java.contract.ContractImport;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import javax.tools.SimpleJavaFileObject;

/**
 * An in-memory Java source file, with mapping between contract model
 * elements and line numbers, representing a contract source.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@ContractImport({
  "com.google.java.contract.util.Iterables",
  "com.google.java.contract.util.Predicates"
})
@Invariant({
  "content != null",
  "lineNumberMap == null " +
      "|| Iterables.all(lineNumberMap.keySet(), Predicates.between(1L, null))"
})
public class SyntheticJavaFile extends SimpleJavaFileObject {
  protected byte[] content;
  protected Map<Long, ?> lineNumberMap;

  /**
   * Constructs a new SyntheticJavaFile. This constructor wraps its
   * argument into a SyntheticJavaFile; the objects are <em>not</em>
   * copied.
   *
   * @param name the class name of the file, in qualified format
   * @param content the content of the file
   * @param lineNumberMap the line number mapping
   */
  @Requires({
    "name != null",
    "content != null"
  })
  public SyntheticJavaFile(String name, byte[] content,
                           Map<Long, ?> lineNumberMap) {
    super(Elements.getUriForClass(name, Kind.SOURCE), Kind.SOURCE);
    this.content = content;
    this.lineNumberMap = lineNumberMap;
  }

  @Override
  public InputStream openInputStream() {
    return new ByteArrayInputStream(content);
  }

  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
    return new String(content);
  }

  @Override
  public boolean delete() {
    content = null;
    return true;
  }

  /**
   * Returns the source information object associated with the
   * specified line number.
   */
  public Object getSourceInfo(long lineNumber) {
    return lineNumberMap == null ? null : lineNumberMap.get(lineNumber);
  }
}
