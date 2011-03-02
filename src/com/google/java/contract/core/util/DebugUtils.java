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
package com.google.java.contract.core.util;

import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.tools.JavaFileObject;

/**
 * Utility methods for debugging.
 */
public class DebugUtils {
  private static String dumpDirectory = "contracts_for_java.out";

  private static Map<String, Boolean> loggingEnabled =
      new HashMap<String, Boolean>();

  @Ensures("result != null")
  public static String getDumpDirectory() {
    return dumpDirectory;
  }

  @Requires("dir != null")
  @Ensures("getDumpDirectory().equals(dir)")
  public static void setDumpDirectory(String dir) {
    dumpDirectory = dir;
  }

  /**
   * Dumps the specified Java file (source, class, etc.) to the dump
   * directory (by default, "contracts_for_java.out" in the current
   * directory).
   *
   * @param name the qualified class name of the file to dump
   * @param data the file content
   * @param kind the kind of file to dump
   */
  @Requires({
    "name != null",
    "data != null",
    "kind != null"
  })
  public static void dump(String name, byte[] data, JavaFileObject.Kind kind) {
    File f = new File(dumpDirectory + "/" + name + kind.extension);
    info("dump", "dumping file " + f);
    f.getParentFile().mkdirs();
    try {
      OutputStream out = new FileOutputStream(f);
      out.write(data);
      out.flush();
      out.close();
    } catch (IOException e) {
      warn("dump", "while dumping " + f + ": " + e.getMessage());
    }
  }

  @Requires("facility != null")
  public static boolean isLoggingEnabled(String facility) {
    Boolean enabled = loggingEnabled.get(facility);
    if (enabled == null) {
      enabled = Boolean.valueOf(
          System.getProperty("com.google.java.contract.log." + facility, "false"));
      loggingEnabled.put(facility, enabled);
    }
    return enabled;
  }

  /**
   * Outputs an informative message regarding contract
   * activation. Does not bear any contracts itself so as not to
   * pollute the output.
   */
  public static void contractInfo(String message) {
    if (isLoggingEnabled("contract")) {
      System.err.println("[com.google.java.contract:contract "
                         + message + "]");
    }
  }

  @Requires({
    "facility != null",
    "message != null"
  })
  public static void info(String facility, String message) {
    if (isLoggingEnabled(facility)) {
      System.err.println("[com.google.java.contract:" + facility + " "
                         + message + "]");
    }
  }

  @Requires({
    "facility != null",
    "message != null"
  })
  public static void warn(String facility, String message) {
    System.err.println("[com.google.java.contract:" + facility + " "
                       + message + "]");
  }

  @Requires({
    "facility != null",
    "message != null"
  })
  public static void err(String facility, String message, Throwable cause) {
    System.err.println("[com.google.java.contract:" + facility + " FATAL ERROR "
                       + message
                       + (cause != null ? " (stack trace follows)" : "")
                       + "]");
    if (cause != null) {
      cause.printStackTrace();
    } else {
      new Exception().printStackTrace();
    }
    System.exit(1);
  }
}
