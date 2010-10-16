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
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.util.DebugUtils;
import com.google.java.contract.core.util.PatternMap;

/**
 * A process-wide collection of contract activation rules.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({
  "assertPre != null",
  "assertPost != null",
  "assertInvariant != null"
})
public class ActivationRuleManager {
  protected static ActivationRuleManager instance = null;

  protected PatternMap<Boolean> assertPre = new PatternMap<Boolean>();
  protected PatternMap<Boolean> assertPost = new PatternMap<Boolean>();
  protected PatternMap<Boolean> assertInvariant = new PatternMap<Boolean>();

  protected ActivationRuleManager() {
    assertPre = new PatternMap<Boolean>();
    assertPost = new PatternMap<Boolean>();
    assertInvariant = new PatternMap<Boolean>();
  }

  public static ActivationRuleManager getInstance() {
    if (instance == null) {
      instance = new ActivationRuleManager();
    }
    return instance;
  }

  @Requires("pattern != null")
  @Ensures("hasPreconditionsEnabled(pattern)")
  public synchronized void enablePreconditions(String pattern) {
    DebugUtils.info("activation", pattern + " +requires");
    assertPre.put(pattern, true);
  }

  @Requires("pattern != null")
  @Ensures("!hasPreconditionsEnabled(pattern)")
  public synchronized void disablePreconditions(String pattern) {
    DebugUtils.info("activation", pattern + " -requires");
    assertPre.put(pattern, false);
  }

  @Requires("pattern != null")
  @Ensures("hasPostconditionsEnabled(pattern)")
  public synchronized void enablePostconditions(String pattern) {
    DebugUtils.info("activation", pattern + " +ensures");
    assertPost.put(pattern, true);
  }

  @Requires("pattern != null")
  @Ensures("!hasPostconditionsEnabled(pattern)")
  public synchronized void disablePostconditions(String pattern) {
    DebugUtils.info("activation", pattern + " -ensures");
    assertPost.put(pattern, false);
  }

  @Requires("pattern != null")
  @Ensures("hasInvariantsEnabled(pattern)")
  public synchronized void enableInvariants(String pattern) {
    DebugUtils.info("activation", pattern + " +invariant");
    assertInvariant.put(pattern, true);
  }

  @Requires("pattern != null")
  @Ensures("!hasInvariantsEnabled(pattern)")
  public synchronized void disableInvariants(String pattern) {
    DebugUtils.info("activation", pattern + " -invariant");
    assertInvariant.put(pattern, false);
  }

  @Requires("pattern != null")
  public synchronized boolean hasPreconditionsEnabled(String pattern) {
    if (pattern.endsWith(".*") && assertPre.isOverriden(pattern)) {
      return false;
    }
    Boolean rule = assertPre.get(pattern);
    return rule == null || rule;
  }

  @Requires("pattern != null")
  public synchronized boolean hasPostconditionsEnabled(String pattern) {
    if (pattern.endsWith(".*") && assertPost.isOverriden(pattern)) {
      return false;
    }
    Boolean rule = assertPost.get(pattern);
    return rule == null || rule;
  }

  @Requires("pattern != null")
  public synchronized boolean hasInvariantsEnabled(String pattern) {
    if (pattern.endsWith(".*") && assertInvariant.isOverriden(pattern)) {
      return false;
    }
    Boolean rule = assertInvariant.get(pattern);
    return rule == null || rule;
  }
}
