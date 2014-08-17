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

import com.google.java.contract.ContractImport;
import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.model.ClassName;
import com.google.java.contract.core.util.BalancedTokenizer;
import com.google.java.contract.core.util.JavaTokenizer;
import com.google.java.contract.core.util.JavaUtils;
import com.google.java.contract.core.util.JavaUtils.ParseException;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A flexible parser that extracts import statements and line number
 * information from a Java source file.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@ContractImport({
  "com.google.java.contract.util.Iterables",
  "com.google.java.contract.util.Predicates"
})
@Invariant({
  "!canQueryResults() || getImportNames() != null",
  "!canQueryResults() || !getImportNames().contains(null)",
  "!canQueryResults() || getContractLineNumbers() != null",
  "!canQueryResults() || !getContractLineNumbers().entrySet().contains(null)",
  "!canQueryResults() || " +
      "Iterables.all(getContractLineNumbers().values(), " +
      "    Predicates.<Long>all(Predicates.between(1L, null)))",
  "source != null"
})
public class SourceDependencyParser {
  /**
   * The source file being parsed.
   */
  protected Reader source;

  /**
   * A set of import targets that are in effect in the source file.
   */
  protected Set<String> importNames;

  /**
   * The successive positions of contracts in the source file, in each
   * top-level class. Each clause has an entry in the list. Each entry
   * for a top-level class includes contracts in nested classes.
   */
  protected Map<ClassName, List<Long>> contractLineNumbers;

  /**
   * Whether {@code source} has been parsed yet.
   */
  protected boolean parsed;

  private static final List<String> TYPE_KEYWORDS =
      Arrays.asList("class", "enum", "interface");
  private static final List<String> CONTRACT_TYPES =
      Arrays.asList("Requires", "Ensures", "ThrowEnsures", "Invariant");

  /**
   * Constructs a new SourceDependencyParser.
   *
   * @param source the source file
   */
  public SourceDependencyParser(Reader source) {
    this.source = source;
    importNames = new HashSet<String>();
    contractLineNumbers = new HashMap<ClassName, List<Long>>();
    parsed = false;
  }

  /**
   * Parses the source file.
   *
   * @throws ParseException if a parsing error occurs
   */
  @Ensures("canQueryResults()")
  public void parse() throws ParseException {
    if (parsed) {
      return;
    }

    try {
      BalancedTokenizer tokenizer = new BalancedTokenizer(source);
      String packageName = null;
      ClassName className = null;
      ArrayList<Long> orphanLineNumbers = new ArrayList<Long>();
      while (tokenizer.hasNext()) {
        JavaTokenizer.Token token = tokenizer.next();
        switch (token.kind) {
          case WORD:
            if (tokenizer.getCurrentLevel() == 0) {
              if (token.text.equals("package")) {
                packageName = JavaUtils.parseQualifiedName(tokenizer);
              } else if (token.text.equals("import")) {
                String name = JavaUtils.parseQualifiedName(tokenizer, true);
                if (name.equals("static")) {
                  name += " " + JavaUtils.parseQualifiedName(tokenizer, true);
                }
                importNames.add(name);
              } else if (TYPE_KEYWORDS.contains(token.text)) {
                String name = JavaUtils.parseQualifiedName(tokenizer);
                if (packageName != null) {
                  name = packageName + "." + name;
                }
                className = new ClassName(name.replace('.', '/'));
                contractLineNumbers
                    .put(className, new ArrayList<Long>(orphanLineNumbers));
                orphanLineNumbers.clear();
                JavaUtils.skipPast(tokenizer, "{");
              }
            }
            break;

          case SYMBOL:
            if (tokenizer.getCurrentLevel() == 0 && token.text.equals("}")) {
              className = null;
            } else {
              if (token.text.equals("@")) {
                String annotationType = JavaUtils.parseQualifiedName(tokenizer);
                if (annotationType.startsWith("com.google.java.contract.")
                    || (CONTRACT_TYPES.contains(annotationType)
                        && ((packageName != null
                             && packageName.equals("com.google.java.contract"))
                            || importNames.contains("com.google.java.contract."
                                                    + annotationType)
                            || importNames.contains("com.google.java.contract.*")))) {
                  List<Long> lineNumbers;
                  if (className != null) {
                    lineNumbers = contractLineNumbers.get(className);
                  } else {
                    lineNumbers = orphanLineNumbers;
                  }
                  parseContractClauses(tokenizer, lineNumbers);
                }
              }
            }
            break;

          default:
            break;
        }
      }
      parsed = true;
    } catch (NoSuchElementException e) {
      throw new ParseException(e);
    }
  }

  @Requires({
    "tokenizer != null",
    "lineNumbers != null"
  })
  private void parseContractClauses(BalancedTokenizer tokenizer,
                                    List<Long> lineNumbers) {
    boolean expectClause = true;
    while (tokenizer.hasNext()) {
      long lineNumber = tokenizer.getCurrentLineNumber();
      JavaTokenizer.Token token = tokenizer.next();
      if (token.text.equals(")")) {
        return;
      } else if (expectClause && token.kind == JavaTokenizer.TokenKind.QUOTE) {
        lineNumbers.add(lineNumber);
        expectClause = false;
      } else if (token.text.equals(",")) {
        expectClause = true;
      }
    }
  }

  public boolean canQueryResults() {
    return parsed;
  }

  @Requires("canQueryResults()")
  public Set<String> getImportNames() {
    return Collections.unmodifiableSet(importNames);
  }

  @Requires("canQueryResults()")
  public Map<ClassName, List<Long>> getContractLineNumbers() {
    return Collections.unmodifiableMap(contractLineNumbers);
  }
}
