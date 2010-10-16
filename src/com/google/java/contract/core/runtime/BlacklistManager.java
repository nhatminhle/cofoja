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
package com.google.java.contract.core.runtime;

import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.core.util.DebugUtils;
import com.google.java.contract.core.util.PatternMap;

/**
 * A process-wide collection of blacklisted classes.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @see com.google.java.contract.ContractEnvironment#ignore(String)
 */
@Invariant("blacklist != null")
public class BlacklistManager {
  protected static BlacklistManager instance = null;

  protected PatternMap<Boolean> blacklist = new PatternMap<Boolean>();

  protected BlacklistManager() {
    blacklist = new PatternMap<Boolean>();
    blacklist.put("java.*", true);
    blacklist.put("javax.*", true);
    blacklist.put("com.sun.*", true);
    blacklist.put("sun.*", true);
  }

  public static BlacklistManager getInstance() {
    if (instance == null) {
      instance = new BlacklistManager();
    }
    return instance;
  }

  @Requires("pattern != null")
  @Ensures("isIgnored(pattern)")
  public synchronized void ignore(String pattern) {
    DebugUtils.info("activation", pattern + " +blacklist");
    blacklist.put(pattern, true);
  }

  @Requires("pattern != null")
  @Ensures("!isIgnored(pattern)")
  public synchronized void unignore(String pattern) {
    DebugUtils.info("activation", pattern + " -blacklist");
    blacklist.put(pattern, false);
  }

  @Requires("pattern != null")
  public synchronized boolean isIgnored(String pattern) {
    if (pattern.endsWith(".*") && blacklist.isOverriden(pattern)) {
      return false;
    }
    Boolean rule = blacklist.get(pattern);
    return rule != null && rule;
  }
}
