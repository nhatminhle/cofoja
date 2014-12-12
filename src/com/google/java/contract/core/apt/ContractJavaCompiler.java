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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * A compiler that handles generated contract source files.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
@Invariant({
  "javaCompiler != null",
  "fileManager != null"
})
public class ContractJavaCompiler {
  private static final String JAVA_VERSION =
      System.getProperty("java.specification.version");

  /**
   * The compiler options passed to the internal compiler.
   */
  protected static final List<String> OPTIONS =
      Arrays.asList(
          "-g:source,vars", /* Source file name, for debug attributes. */
          "-proc:none",     /* No further annotations to process. */
          "-implicit:none", /* No class files for implicit dependencies. */
          /* Target the highest Java version supported by the runtime, instead
           * of the highest supported by the compiler. */
          "-source", JAVA_VERSION,
          "-target", JAVA_VERSION
      );

  /**
   * Internal compiler to delegate to.
   */
  protected JavaCompiler javaCompiler;

  protected ContractJavaFileManager fileManager;

  public ContractJavaCompiler(String sourcePath, String classPath,
                              String outputDirectory)
      throws IOException {
    javaCompiler = ToolProvider.getSystemJavaCompiler();
    if (javaCompiler == null) {
      throw new IOException("no system JavaCompiler found; "
                            + "are you using a JRE instead of a JDK?");
    }

    fileManager = new ContractJavaFileManager(
        javaCompiler.getStandardFileManager(null, null, null));

    if (sourcePath != null) {
      setPath(StandardLocation.SOURCE_PATH, sourcePath);
    }
    if (classPath != null) {
      setPath(StandardLocation.CLASS_PATH, classPath);
    }
    if (outputDirectory != null) {
      setClassOutputDirectory(outputDirectory);
    }
  }

  /**
   * Returns a new compilation task.
   */
  @Requires({
    "files != null",
    "diagnostics != null"
  })
  @Ensures("result != null")
  public CompilationTask getTask(List<? extends JavaFileObject> files,
      DiagnosticListener<JavaFileObject> diagnostics) {
    return javaCompiler.getTask(null, fileManager, diagnostics,
                                OPTIONS, null, files);
  }

  @Requires({
    "location != null",
    "path != null"
  })
  protected void setPath(Location location, String path) throws IOException {
    String[] parts = path.split(Pattern.quote(File.pathSeparator));
    ArrayList<File> dirs = new ArrayList<File>(parts.length);
    for (String part : parts) {
      dirs.add(new File(part));
    }
    fileManager.setLocation(location, dirs);
  }

  @Requires("outputDirectory != null")
  protected void setClassOutputDirectory(String outputDirectory)
      throws IOException {
    fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
        Collections.singletonList(new File(outputDirectory)));
  }
}
