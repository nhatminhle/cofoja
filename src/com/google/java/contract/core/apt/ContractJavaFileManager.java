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

import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ClassName;
import com.google.java.contract.core.util.Elements;
import com.google.java.contract.core.util.JavaUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * A file manager that handles output (class) files from contract
 * compilation. Class files are written in the configured class output
 * directory (usually alongside other class files).
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
public class ContractJavaFileManager
    extends ForwardingJavaFileManager<StandardJavaFileManager> {
  /**
   * An output file to a plain contract class file.
   */
  @Invariant("file != null")
  protected class SimpleOutputJavaFileObject extends SimpleJavaFileObject {
    protected FileObject file;

    @Requires({
      "binaryName != null",
      "file != null"
    })
    public SimpleOutputJavaFileObject(String binaryName, FileObject file) {
      super(Elements.getUriForClass(binaryName, Kind.CLASS), Kind.CLASS);
      this.file = file;
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
      return file.openOutputStream();
    }
  }

  /**
   * Constructs a new ContractJavaFileManager writing files to
   * {@code fileManager}.
   */
  @Requires("fileManager != null")
  public ContractJavaFileManager(StandardJavaFileManager fileManager) {
    super(fileManager);
  }

  @Override
  public JavaFileObject getJavaFileForOutput(Location location,
      String className, Kind kind, FileObject sibling)
      throws IOException {
    String binaryName = className.replace('.', '/');
    String relativeName = ClassName.getRelativeName(className);
    if (relativeName.endsWith(JavaUtils.HELPER_CLASS_SUFFIX)) {
      relativeName += Kind.CLASS.extension;
    } else {
      relativeName += JavaUtils.CONTRACTS_EXTENSION;
    }
    FileObject file =
        fileManager.getFileForOutput(location,
                                     ClassName.getPackageName(className),
                                     relativeName, sibling);
    return new SimpleOutputJavaFileObject(binaryName, file);
  }

  /**
   * Returns a list of paths associated with {@code location}, or
   * {@code null}.
   */
  @Requires("location != null")
  public List<? extends File> getLocation(Location location) {
    Iterable<? extends File> path = fileManager.getLocation(location);
    if (path == null) {
      return null;
    }

    ArrayList<File> locations = new ArrayList<File>();
    for (File entry : path) {
      locations.add(entry);
    }
    return locations;
  }

  /**
   * Sets the list of paths associated with {@code location}.
   *
   * @param location the affected location
   * @param path a list of paths, or {@code null} to reset to default
   */
  @Requires("location != null")
  public void setLocation(Location location, List<? extends File> path)
      throws IOException {
    fileManager.setLocation(location, path);
  }
}
