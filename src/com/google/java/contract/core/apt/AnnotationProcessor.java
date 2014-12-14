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

import com.google.java.contract.ContractImport;
import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ContractAnnotationModel;
import com.google.java.contract.core.model.TypeModel;
import com.google.java.contract.core.util.DebugUtils;
import com.google.java.contract.core.util.ElementScanner;
import com.google.java.contract.core.util.SyntheticJavaFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementScanner6;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;

/**
 * A JSR 269 annotation processor that builds contract Java source
 * files from annotated classes, and compiles them.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 * @author chatain@google.com (Leonardo Chatain)
 */
@ContractImport("javax.lang.model.element.ElementKind")
@SupportedAnnotationTypes("*")
@SupportedOptions({
  AnnotationProcessor.OPT_DEBUG,
  AnnotationProcessor.OPT_DUMP,
  AnnotationProcessor.OPT_SOURCEPATH,
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
   * This option sets the source path for the compilation of the
   * generated source files. It should be the same as the class path
   * for the sources themselves.
   */
  protected static final String OPT_SOURCEPATH = "com.google.java.contract.sourcepath";

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
  protected FactoryUtils utils;

  protected String sourcePath;
  protected String classPath;
  protected String outputDirectory;

  protected boolean debug;
  protected boolean dump;

  private Class<?> javacProcessingEnvironmentClass;
  private Method getContextMethod;
  private Method optionsInstanceMethod;
  private Method optionsGetMethod;

  /**
   * Initialize classes and methods needed for OpenJDK javac reflection.
   */
  private void setupReflection() {
    try {
      javacProcessingEnvironmentClass = Class.forName(
          "com.sun.tools.javac.processing.JavacProcessingEnvironment");
    } catch (ClassNotFoundException e) {
      /* Javac isn't on the classpath. */
      return;
    }

    try {
      Class<?> contextClass = Class.forName("com.sun.tools.javac.util.Context");
      getContextMethod = javacProcessingEnvironmentClass.getMethod("getContext");
      Class<?> optionsClass = Class.forName("com.sun.tools.javac.util.Options");
      optionsInstanceMethod = optionsClass.getMethod("instance", contextClass);
      optionsGetMethod = optionsClass.getMethod("get", String.class);
    } catch (Exception e) {
      throw new LinkageError(e.getMessage());
    }
  }

  /**
   * Calls {@code com.sun.tools.javac.util.Options.get(String)} reflectively.
   */
  private String getJavacOption(Object options, String name) {
    try {
      return (String) optionsGetMethod.invoke(options, name);
    } catch (Exception e) {
      throw new LinkageError(e.getMessage());
    }
  }

  /**
   * Calls {@code com.sun.tools.javac.util.Options.instance(Context)}
   * reflectively.
   */
  private Object getJavacOptions() {
    if (!javacProcessingEnvironmentClass.isInstance(processingEnv)) {
      return null;
    }
    try {
      return optionsInstanceMethod.invoke(null,
          getContextMethod.invoke(processingEnv));
    } catch (Exception e) {
      throw new LinkageError(e.getMessage());
    }
  }

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

    utils = new FactoryUtils(processingEnv);
    factory = new TypeFactory(utils, options.get(OPT_DEPSPATH));

    setupReflection();
    setupPaths();
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations,
                         RoundEnvironment roundEnv) {
    Set<TypeElement> rootElements = getContractedRootElements(roundEnv);
    if (rootElements.isEmpty()) {
      return false;
    }

    DiagnosticManager diagnosticManager = new DiagnosticManager();

    List<TypeModel> types = createTypes(rootElements, diagnosticManager);
    boolean success = diagnosticManager.getErrorCount() == 0;

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

      try {
        ContractJavaCompiler compiler =
            new ContractJavaCompiler(sourcePath, classPath, outputDirectory);
        CompilationTask task = compiler.getTask(sources, diagnosticManager);
        success = task.call();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    if (!success || diagnosticManager.getCount() != 0) {
      for (DiagnosticManager.Report r : diagnosticManager) {
        printDiagnostic(r);
      }
    }

    return true;
  }

  /**
   * Sets class and output paths from command-line options.
   */
  private void setupPaths() {
    sourcePath = processingEnv.getOptions().get(OPT_SOURCEPATH);
    classPath = processingEnv.getOptions().get(OPT_CLASSPATH);
    outputDirectory = processingEnv.getOptions().get(OPT_CLASSOUTPUT);

    /*
     * Load classes in com.sun.tools reflectively for graceful fallback when
     * the OpenJDK javac isn't available (e.g. with J9, or ecj).
     */
    Object options = getJavacOptions();
    if (options == null) {
      return;
    }

    if (sourcePath == null) {
      sourcePath = getJavacOption(options, "-sourcepath");
    }

    if (classPath == null) {
      String classPath1 = getJavacOption(options, "-cp");
      String classPath2 = getJavacOption(options, "-classpath");
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
      outputDirectory = getJavacOption(options, "-d");
    }
  }

  /**
   * Prints {@code diag} with any additional contract code information
   * available.
   */
  @Requires("r != null")
  protected void printDiagnostic(DiagnosticManager.Report r) {
    Messager messager = processingEnv.getMessager();
    if (r.getElement() == null) {
      messager.printMessage(r.getKind(), r.getMessage(null));
    } else {
      messager.printMessage(r.getKind(), r.getMessage(null), r.getElement(),
                            r.getAnnotationMirror(), r.getAnnotationValue());
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
      DebugUtils.dump(type.getName().getBinaryName(),
                      file.getCharContent(true).toString().getBytes(),
                      Kind.SOURCE);
    }
  }

  /**
   * Builds {@link TypeModel} objects from the {@code roots}. The
   * types built contain contract methods. Helper types are created
   * for interfaces.
   */
  @Requires({
    "roots != null",
    "diagnosticManager != null"
  })
  @Ensures({
    "result != null",
    "result.size() >= roots.size()",
    "result.size() <= 2 * roots.size()"
  })
  protected List<TypeModel> createTypes(Set<TypeElement> roots,
                                        DiagnosticManager diagnosticManager) {
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
      TypeModel type = factory.createType(r, diagnosticManager);
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
      ClassContractCreator creator =
          new ClassContractCreator(diagnosticManager);
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
   * Returns the set of root elements that contain contracts.
   * Contracts can have been directly declared as annotations or inherited
   * through the hierarchy.
   *
   * @param annotations the set of annotations to look for
   * @param roundEnv the environment to get elements from
   */
  @Requires("roundEnv != null")
  @Ensures("result != null")
  protected Set<TypeElement> getContractedRootElements(
      RoundEnvironment roundEnv) {
    Set<? extends Element> allElements = roundEnv.getRootElements();
    Set<TypeElement> contractedRootElements =
        new HashSet<TypeElement>(allElements.size());

    ContractFinder cf = new ContractFinder(utils);
    for (Element e : allElements) {
      if (e.accept(cf, null)) {
        contractedRootElements.add(getRootElement(e));
      }
    }

    return contractedRootElements;
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
