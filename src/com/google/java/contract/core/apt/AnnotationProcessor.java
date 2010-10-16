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

import com.sun.tools.javac.main.OptionName;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Options;
import com.google.java.contract.AllowUnusedImport;
import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ContractAnnotationModel;
import com.google.java.contract.core.model.TypeModel;
import com.google.java.contract.core.util.DebugUtils;
import com.google.java.contract.core.util.ElementScanner;
import com.google.java.contract.core.util.JavaUtils;
import com.google.java.contract.core.util.SyntheticJavaFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementScanner6;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

/**
 * A JSR 269 annotation processor that builds contract Java source
 * files from annotated classes, and compiles them.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
@AllowUnusedImport(ElementKind.class)
@SupportedAnnotationTypes({
  "com.google.java.contract.Contracted",
  "com.google.java.contract.Ensures",
  "com.google.java.contract.Invariant",
  "com.google.java.contract.Requires",
  "com.google.java.contract.ThrowEnsures"
})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions({
  AnnotationProcessor.OPT_DEBUG,
  AnnotationProcessor.OPT_DUMP,
  AnnotationProcessor.OPT_CLASSPATH,
  AnnotationProcessor.OPT_CLASSOUTPUT,
  AnnotationProcessor.OPT_DEPSPATH,
  AnnotationProcessor.OPT_EXPERIMENTAL
})
public class AnnotationProcessor extends AbstractProcessor {
  /**
   * This option adds instructions for a debug trace to contract
   * code. For the trace to actually appear at run time, the
   * {@code com.google.java.contract.log.contract} property must be {@code true}.
   */
  protected static final String OPT_DEBUG = "com.google.java.contract.debug";

  /**
   * This option dumps the generated Java source files in the
   * specified directory (defaults to {@code contracts_for_java.out}).
   */
  protected static final String OPT_DUMP = "com.google.java.contract.dump";

  /**
   * This option sets the class path for the compilation of the
   * generated source files. It should be the same as the class path
   * for the sources themselves.
   */
  protected static final String OPT_CLASSPATH = "com.google.java.contract.classpath";

  /**
   * This option sets the class output path for the compilation of the
   * generated source files.
   */
  protected static final String OPT_CLASSOUTPUT = "com.google.java.contract.classoutput";

  /**
   * This option sets the path for source dependency auxiliary files.
   *
   * @see SourcePreprocessor
   */
  protected static final String OPT_DEPSPATH = "com.google.java.contract.depspath";

  /**
   * This option enables experimental Contracts for Java features. <em>These
   * features may or may not be included in future releases, or
   * change, without warning.</em>
   */
  protected static final String OPT_EXPERIMENTAL = "com.google.java.contract.experimental";

  protected TypeFactory factory;

  protected String classPath;
  protected String outputDirectory;

  protected boolean debug;
  protected boolean dump;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);

    Map<String, String> options = processingEnv.getOptions();
    debug = options.containsKey(OPT_DEBUG);
    dump = options.containsKey(OPT_DUMP);
    String dumpDir = options.get(OPT_DUMP);
    if (dumpDir != null) {
      DebugUtils.setDumpDirectory(dumpDir);
    }

    factory = new TypeFactory(processingEnv, options.get(OPT_DEPSPATH));

    setupPaths();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations,
                         RoundEnvironment roundEnv) {
    Set<TypeElement> rootElements =
        getAnnotatedRootElements(annotations, roundEnv);
    if (rootElements.isEmpty()) {
      return false;
    }

    boolean success = true;
    DiagnosticCollector<JavaFileObject> diagnostics =
        new DiagnosticCollector<JavaFileObject>();

    List<TypeModel> types = createTypes(rootElements, diagnostics);
    for (Diagnostic<? extends JavaFileObject> diag :
         diagnostics.getDiagnostics()) {
      if (diag.getKind() == Diagnostic.Kind.ERROR) {
        success = false;
        break;
      }
    }

    ArrayList<SyntheticJavaFile> sources =
        new ArrayList<SyntheticJavaFile>(types.size());
    if (success) {
      for (TypeModel type : types) {
        ContractWriter writer = new ContractWriter(debug);
        type.accept(writer);
        sources.add(new SyntheticJavaFile(type.getName().getBinaryName(),
                                          writer.toByteArray(),
                                          writer.getLineNumberMap()));
      }

      if (dump) {
        dumpSources(types, sources);
      }
    }

    if (success) {
      try {
        ContractJavaCompiler compiler =
            new ContractJavaCompiler(classPath, outputDirectory);
        CompilationTask task = compiler.getTask(sources, diagnostics);
        success = task.call();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    List<Diagnostic<? extends JavaFileObject>> diags =
        diagnostics.getDiagnostics();
    if (!success || !diags.isEmpty()) {
      for (Diagnostic<? extends JavaFileObject> diag : diags) {
        printDiagnostic(diag);
      }
    }

    return true;
  }

  /**
   * Sets class and output paths from command-line options.
   */
  private void setupPaths() {
    classPath = processingEnv.getOptions().get(OPT_CLASSPATH);
    outputDirectory = processingEnv.getOptions().get(OPT_CLASSOUTPUT);

    /*
     * Not using instanceof here because than in every case the JVM
     * tries to load the JavacProcessingEnvironment-class file which,
     * for instance, is not possible with an IBM JVM.
     *
     * TODO(lenh): This may not work; use reflection call instead.
     */
    if (processingEnv.getClass().getName().equals(
            "com.sun.tools.javac.processing.JavacProcessingEnvironment")) {
      JavacProcessingEnvironment javacEnv =
          (JavacProcessingEnvironment) processingEnv;
      Options options = Options.instance(javacEnv.getContext());

      if (classPath == null) {
        String classPath1 = options.get(OptionName.CP);
        String classPath2 = options.get(OptionName.CLASSPATH);
        if (classPath1 != null) {
          if (classPath2 != null) {
            classPath = classPath1 + File.pathSeparator + classPath2;
          } else {
            classPath = classPath1;
          }
        } else {
          classPath = classPath2;
        }
      }

      if (outputDirectory == null) {
        outputDirectory = options.get(OptionName.D);
      }
    }
  }

  /**
   * Prints {@code diag} with any additional contract code information
   * available.
   */
  @Requires("diagnostic != null")
  protected void printDiagnostic(
      Diagnostic<? extends JavaFileObject> diagnostic) {
    Messager messager = processingEnv.getMessager();
    Object info = null;
    if (diagnostic instanceof ContractDiagnostic) {
      ContractDiagnostic contractDiag = (ContractDiagnostic) diagnostic;
      info = contractDiag.getSourceInfo();
    } else if (diagnostic.getSource() instanceof SyntheticJavaFile) {
      SyntheticJavaFile source = (SyntheticJavaFile) diagnostic.getSource();
      info = source.getSourceInfo(diagnostic.getLineNumber());
    }

    if (info == null || !(info instanceof AnnotationSourceInfo)) {
      messager.printMessage(diagnostic.getKind(), diagnostic.getMessage(null));
    } else {
      JavaFileObject source = diagnostic.getSource();
      AnnotationSourceInfo sourceInfo = (AnnotationSourceInfo) info;

      String code = null;
      if (diagnostic.getPosition() != Diagnostic.NOPOS) {
        try {
          CharSequence chars = source.getCharContent(true);
          code = formatErrorSnippet(diagnostic, chars, sourceInfo);
        } catch (IOException e) {
          /* No source code available. */
        }
      }

      String msg = "error in contract: ";

      /*
       * This translates confusing "'<token>' expected" messages from
       * javac to plain "syntax error" messages.
       *
       * TODO(lenh): Think of a more generic way to handle
       * compiler-specific error message rewriting.
       */
      String errorCode = diagnostic.getCode();
      if (errorCode != null && errorCode.startsWith("compiler.err.expected")) {
        msg += "syntax error";
      } else {
        msg += diagnostic.getMessage(null);
      }

      if (code != null) {
        msg += "\n" + code;
      }

      messager.printMessage(diagnostic.getKind(), msg,
                            sourceInfo.getElement(),
                            sourceInfo.getAnnotationMirror(),
                            sourceInfo.getAnnotationValue());
    }
  }

  /**
   * Formats a pretty-printed snippet around where the error
   * occurred. The returned snippet has the faulty sequence
   * underlined, if possible.
   */
  @Requires({
    "diagnostic != null",
    "sourceContent != null",
    "sourceInfo != null"
  })
  protected String formatErrorSnippet(
      Diagnostic<? extends JavaFileObject> diagnostic,
      CharSequence sourceContent, AnnotationSourceInfo sourceInfo) {
    int column = (int) diagnostic.getColumnNumber();

    /*
     * Determine length of extra context to ensure we match the code
     * expressions entirely.
     */
    int maxCodeLength = 0;
    for (String expr : sourceInfo.getCode()) {
      int length = expr.length();
      if (length > maxCodeLength) {
        maxCodeLength = length;
      }
    }

    /* Fetch context. */
    int errorPos = (int) diagnostic.getPosition();
    int lineStart = errorPos - column + 1;
    int errorStart = (int) diagnostic.getStartPosition();
    int errorEnd = (int) diagnostic.getEndPosition();
    String partialLine = sourceContent
        .subSequence(lineStart, errorEnd)
        .toString();

    /* Match failed expression. */
    String snippet = null;
    int snippetErrorPos = -1;
    int snippetErrorStart = -1;
    int snippetErrorEnd = -1;
    for (String expr : sourceInfo.getCode()) {
      /* Find debug marker. */
      String commentMarker =
          JavaUtils.BEGIN_LOCATION_COMMENT
          + JavaUtils.quoteComment(expr)
          + JavaUtils.END_LOCATION_COMMENT;
      int pos = partialLine.lastIndexOf(commentMarker);
      if (pos != -1) {
        snippet = expr;
        pos += commentMarker.length();

        /* Compute generated code offsets. */
        int base = lineStart + pos;
        snippetErrorPos = errorPos - base;
        snippetErrorStart = errorStart - base;
        snippetErrorEnd = errorEnd - base;

        snippetErrorPos -= JavaUtils.generatedCodeLength(
            partialLine.substring(pos, pos + snippetErrorPos));
        snippetErrorStart -= JavaUtils.generatedCodeLength(
            partialLine.substring(pos, pos + snippetErrorStart));
        snippetErrorEnd -= JavaUtils.generatedCodeLength(
            partialLine.substring(pos, pos + snippetErrorEnd));
      }
    }

    if (snippet != null) {
      StringBuilder buffer = new StringBuilder("clause: ");
      buffer.append(snippet);
      if (snippetErrorPos != -1) {
        buffer.append("\n        ");
        int end = snippet.length();
        if (snippetErrorPos > end) {
          snippetErrorPos = end;
        }
        if (snippetErrorStart > end) {
          snippetErrorStart = end;
        }
        if (snippetErrorEnd > end) {
          snippetErrorEnd = end;
        }
        underlineError(buffer, snippet, snippetErrorPos,
                       snippetErrorStart, snippetErrorEnd);
        snippet = buffer.toString();
      }
    }
    return snippet;
  }

  /**
   * Returns a string with the faulty part underlined.
   */
  @Requires({
    "buffer != null",
    "expr != null",
    "pos >= start",
    "pos <= end",
    "start >= 0",
    "start <= end",
    "end <= expr.length()"
  })
  protected void underlineError(StringBuilder buffer, String expr,
                                int pos, int start, int end) {
    int i = 0;
    for (; i < start; ++i) {
      buffer.append(" ");
    }

    if (pos == start) {
      buffer.append("^");
    } else {
      for (; i < pos; ++i) {
        buffer.append("~");
      }
      buffer.append("^");
      ++i;
      for (; i < end; ++i) {
        buffer.append("~");
      }
    }
  }

  /**
   * Dumps the computed Java source files in the dump directory of
   * Contracts for Java.
   */
  @Requires({
     "types != null",
     "sources != null",
     "types.size() == sources.size()"
  })
  protected void dumpSources(List<TypeModel> types,
                             List<SyntheticJavaFile> sources) {
    Iterator<TypeModel> itType = types.iterator();
    Iterator<SyntheticJavaFile> itFile = sources.iterator();
    while (itType.hasNext() && itFile.hasNext()) {
      TypeModel type = itType.next();
      SyntheticJavaFile file = itFile.next();
      try {
        DebugUtils.dump(type.getName().getBinaryName(),
                        file.getCharContent(true).toString().getBytes(),
                        Kind.SOURCE);
      } catch (IOException e) {
        DebugUtils.warn("dump", "while reading: " + file.getName());
      }
    }
  }

  /**
   * Builds {@link TypeModel} objects from the {@code roots}. The
   * types built contain contract methods. Helper types are created
   * for interfaces.
   */
  @Requires({
    "roots != null",
    "diagnostics != null"
  })
  @Ensures({
    "result != null",
    "result.size() >= roots.size()",
    "result.size() <= 2 * roots.size()"
  })
  protected List<TypeModel> createTypes(Set<TypeElement> roots,
      DiagnosticCollector<JavaFileObject> diagnostics) {
    boolean errors = false;

    /*
     * Extract all type names that will be part of this compilation
     * task.
     */
    final HashSet<String> knownTypeNames = new HashSet<String>();
    for (TypeElement r : roots) {
      ElementScanner6<Void, Void> visitor =
          new ElementScanner6<Void, Void>() {
            @Override
            public Void visitType(TypeElement e, Void p) {
              knownTypeNames.add(e.getQualifiedName().toString());
              return super.visitType(e, p);
            }
          };
      r.accept(visitor, null);
    }

    /*
     * Mark annotations inherited from classes compiled in the same
     * task as weak so we don't generate stubs for them later on. This
     * prevents name clashes due to erasure.
     */
    ArrayList<TypeModel> undecoratedTypes =
        new ArrayList<TypeModel>(roots.size());
    for (TypeElement r : roots) {
      TypeModel type = factory.createType(r);
      ElementScanner annotator =
          new ElementScanner() {
            @Override
            public void visitContractAnnotation(
                ContractAnnotationModel annotation) {
              if (annotation.isVirtual()
                  && knownTypeNames.contains(
                      annotation.getOwner().getQualifiedName())) {
                annotation.setWeakVirtual(true);
              }
            }
          };
      type.accept(annotator);
      undecoratedTypes.add(type);
    }

    /*
     * Decorate the type models with contract methods and create
     * helper types.
     */
    ArrayList<TypeModel> types =
        new ArrayList<TypeModel>(undecoratedTypes.size());
    for (TypeModel type : undecoratedTypes) {
      ClassContractCreator creator = new ClassContractCreator(diagnostics);
      type.accept(creator);
      TypeModel helper = creator.getHelperType();
      types.add(type);
      if (helper != null) {
        types.add(helper);
      }
    }

    return types;
  }

  /**
   * Returns the set of root elements annotated with any annotation in
   * {@code annotations}, or that have descendants annotated with
   * these annotations.
   *
   * @param annotations the set of annotations to look for
   * @param roundEnv the environment to get elements from
   */
  @Requires({
    "annotations != null",
    "roundEnv != null"
  })
  @Ensures("result != null")
  protected static Set<TypeElement> getAnnotatedRootElements(
      Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv) {
    HashSet<TypeElement> rootElements = new HashSet<TypeElement>();
    for (TypeElement annotation : annotations) {
      Set<? extends Element> elements =
          roundEnv.getElementsAnnotatedWith(annotation);
      for (Element e : elements) {
        rootElements.add(getRootElement(e));
      }
    }

    return rootElements;
  }

  /**
   * Returns the top-level type element enclosing {@code element}.
   */
  @Requires({
    "element != null",
    "element.getKind() != ElementKind.PACKAGE",
    "element.getKind() != ElementKind.OTHER"
  })
  @Ensures("element != null")
  protected static TypeElement getRootElement(Element element) {
    if (element.getKind().isClass() || element.getKind().isInterface()) {
      TypeElement type = (TypeElement) element;
      if (!type.getNestingKind().isNested()) {
        return type;
      }
    }

    return getRootElement(element.getEnclosingElement());
  }
}
