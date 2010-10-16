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
package com.google.java.contract.core.apt;

import com.google.java.contract.core.model.ClassName;
import com.google.java.contract.core.util.JavaUtils;
import com.google.java.contract.core.util.JavaUtils.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.JavaFileObject.Kind;

/**
 * Main class of Contracts for Java source dependency preprocessor.
 *
 * <p>The program takes a list of file names as input and produces
 * matching source dependency files as output. If the system property
 * {@code com.google.java.contract.depsoutput} is defined, then dependency files
 * are output in that directory, following the natural hierarchy of
 * the Java classes.
 *
 * <p>If a source file name does not end with the {@code .java}
 * extension, that extension is appended to the file name.
 *
 * <p>Source dependency files contain information about import
 * statements in effect in the corresponding source file as well as
 * line numbering information for contracts. They can be provided to
 * the annotation processor through the {@code com.google.java.contract.depspath}
 * option.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class SourcePreprocessor {
  public static void main(String[] args)
      throws IOException, ParseException {
    String depout = System.getProperty("com.google.java.contract.depsoutput");
    for (String arg : args) {
      if (arg.startsWith("-")) {
        continue;
      }

      String baseName = arg;
      if (arg.endsWith(Kind.SOURCE.extension)) {
        baseName = baseName
            .substring(0, baseName.length() - Kind.SOURCE.extension.length());
      }

      File fileName = new File(baseName + Kind.SOURCE.extension);
      String dir = fileName.getParent();
      dir = dir == null ? "" : dir + "/";

      FileInputStream in = new FileInputStream(arg);

      SourceDependencyParser parser =
          new SourceDependencyParser(new InputStreamReader(in));
      try {
        parser.parse();
      } catch (ParseException e) {
        throw new ParseException(
            fileName + " is malformed; "
            + "you should not compile contracts before compiling "
            + "the actual source files; "
            + "if this file is valid Java, you found a bug in Contracts for Java; "
            + "please email 'davidmorgan@google.com'",
            e);
      }

      Set<String> importNames = parser.getImportNames();
      Map<ClassName, List<Long>> contractLineNumbers =
          parser.getContractLineNumbers();

      if (contractLineNumbers.isEmpty()) {
        continue;
      }

      for (Map.Entry<ClassName, List<Long>> entry :
           contractLineNumbers.entrySet()) {
        ClassName className = entry.getKey();
        File outputFileName;
        if (depout == null) {
          outputFileName = new File(dir + className.getSimpleName()
                                    + JavaUtils.SOURCE_DEPENDENCY_EXTENSION);
        } else {
          outputFileName = new File(depout + "/" + className.getBinaryName()
                                    + JavaUtils.SOURCE_DEPENDENCY_EXTENSION);
        }

        outputFileName.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(outputFileName);
        ObjectOutputStream oout = new ObjectOutputStream(out);

        oout.writeObject(importNames);
        oout.writeObject(entry.getValue());
        oout.close();
      }

      in.close();
    }
  }
}
