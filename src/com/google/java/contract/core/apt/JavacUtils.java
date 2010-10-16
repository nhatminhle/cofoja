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

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/**
 * A com.sun.source-based utility class that extracts source
 * information.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
class JavacUtils {
  /**
   * Returns the line numbers associated with {@code annotation}.
   */
  @Requires({
    "processingEnv != null",
    "element != null",
    "annotation != null"
  })
  @Ensures("result != null")
  public static List<Long> getLineNumbers(ProcessingEnvironment processingEnv,
      Element element, AnnotationMirror annotation) {
    Trees treeUtils = Trees.instance(processingEnv);
    if (treeUtils == null) {
      return Collections.emptyList();
    }

    TreePath path = treeUtils.getPath(element, annotation);
    if (path == null) {
      return Collections.emptyList();
    }

    CompilationUnitTree unitTree = path.getCompilationUnit();
    LineMap lineMap = unitTree.getLineMap();
    SourcePositions positions = treeUtils.getSourcePositions();

    AnnotationTree annotationTree = (AnnotationTree) path.getLeaf();
    AssignmentTree assignTree =
        (AssignmentTree) annotationTree.getArguments().get(0);
    ExpressionTree exprTree = assignTree.getExpression();

    ArrayList<Long> lines = new ArrayList<Long>();
    if (exprTree.getKind() == Kind.STRING_LITERAL) {
      long pos = positions.getStartPosition(unitTree, exprTree);
      lines.add(lineMap.getLineNumber(pos));
    } else {
      NewArrayTree valuesTree = (NewArrayTree) exprTree;
      for (ExpressionTree valueTree : valuesTree.getInitializers()) {
        long pos = positions.getStartPosition(unitTree, valueTree);
        lines.add(lineMap.getLineNumber(pos));
      }
    }

    return lines;
  }

  /**
   * Returns the import statements in effect in the containing
   * compilation unit of {@code element}.
   */
  @Requires({
    "processingEnv != null",
    "element != null"
  })
  @Ensures("result != null")
  public static Set<String> getImportNames(ProcessingEnvironment processingEnv,
      Element element) {
    Trees treeUtils = Trees.instance(processingEnv);
    if (treeUtils == null) {
      return Collections.emptySet();
    }

    TreePath path = treeUtils.getPath(element);
    if (path == null) {
      return Collections.emptySet();
    }

    CompilationUnitTree unitTree = path.getCompilationUnit();

    HashSet<String> importNames = new HashSet<String>();
    for (ImportTree importTree : unitTree.getImports()) {
      StringBuilder buffer = new StringBuilder();
      if (importTree.isStatic()) {
        buffer.append("static ");
      }
      /* TODO(lenh): Roll our own toString()? */
      buffer.append(importTree.getQualifiedIdentifier().toString());
      importNames.add(buffer.toString());
    }

    return importNames;
  }
}
