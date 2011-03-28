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

import com.google.java.contract.Contracted;
import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;
import com.google.java.contract.SpecificationError;
import com.google.java.contract.ThrowEnsures;
import com.google.java.contract.core.util.DebugUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A singleton that manages checks for missing {@link Contracted}
 * annotations.
 *
 * <p>This class <em>must</em> be separate from
 * {@link ContractRuntime} since we rely on the latter's static
 * initializer to automatically run the check upon execution of
 * the first contract.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class ContractedChecker {
  protected static ContractedChecker instance = null;

  static boolean startupContractedCheck = true;

  protected ConcurrentHashMap<Class<?>, Boolean> declaredContracted =
      new ConcurrentHashMap<Class<?>, Boolean>();
  protected ConcurrentHashMap<Class<?>, Boolean> contracted =
      new ConcurrentHashMap<Class<?>, Boolean>();
  protected LinkedBlockingDeque<String> futureContractedChecks =
      new LinkedBlockingDeque<String>();

  protected ContractedChecker() {
    declaredContracted = new ConcurrentHashMap<Class<?>, Boolean>();
    contracted = new ConcurrentHashMap<Class<?>, Boolean>();
    futureContractedChecks = new LinkedBlockingDeque<String>();
  }

  public static ContractedChecker getInstance() {
    if (instance == null) {
      instance = new ContractedChecker();
    }
    return instance;
  }

  public static void disableStartupContractedCheck() {
    startupContractedCheck = false;
  }

  public void addFutureContractedCheck(String className) {
    futureContractedChecks.add(className);
  }

  /**
   * Returns {@code true} if the specified class (or any of its
   * methods) has at least one contract annotation. This method does
   * not check parents.
   */
  public boolean hasDeclaredContracts(Class<?> clazz) {
    Boolean result = declaredContracted.get(clazz);
    if (result != null) {
      return result;
    }

    result = false;

    if (clazz.getAnnotation(Contracted.class) != null
        || clazz.getAnnotation(Invariant.class) != null) {
      result = true;
    }

    if (!result) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (method.getAnnotation(Ensures.class) != null
            || method.getAnnotation(Requires.class) != null
            || method.getAnnotation(ThrowEnsures.class) != null) {
          result = true;
          break;
        }
      }
    }

    if (!result) {
      for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
        if (constructor.getAnnotation(Ensures.class) != null
            || constructor.getAnnotation(Requires.class) != null
            || constructor.getAnnotation(ThrowEnsures.class) != null) {
          result = true;
          break;
        }
      }
    }

    declaredContracted.put(clazz, result);
    return result;
  }

  /**
   * Returns {@code true} if {@code clazz} or any of its (direct or
   * indirect) parents have at least one contract annotation.
   */
  public boolean hasContracts(Class<?> clazz) {
    Boolean result = contracted.get(clazz);
    if (result != null) {
      return result;
    }

    result = false;

    if (hasDeclaredContracts(clazz)) {
      result = true;
    } else if (clazz.getSuperclass() != null
               && hasContracts(clazz.getSuperclass())) {
      result = true;
    } else {
      for (Class<?> iface : clazz.getInterfaces()) {
        if (hasContracts(iface)) {
          result = true;
          break;
        }
      }
    }

    contracted.put(clazz, result);
    return result;
  }

  /**
   * Throws a RuntimeException if {@code clazz} (directly or indirectly)
   * extends a contracted class, but is not declared contracted
   * itself.
   *
   * <p>Classes that do not include any contract annotation are not
   * processed by the contract compiler and hence do not inherit
   * contracts properly.
   */
  public void assertContracted(Class<?> clazz) throws SpecificationError {
    if (!isSynthetic(clazz)
        && !hasDeclaredContracts(clazz) && hasContracts(clazz)) {
      throw new SpecificationError(clazz
          + ": must be annotated with 'com.google.java.contract.Contracted'; "
          + "this class has inherited contracts but "
          + "does not specify contracts itself directly: "
          + "without the 'com.google.java.contract.Contracted' annotation, "
          + "its inherited contracts would not be enforced");
    }
  }

  private boolean isSynthetic(Class<?> clazz) {
    return clazz.isSynthetic() || clazz.getName().contains("$$");
  }

  /**
   * Disables the automatic check for missing
   * {@link com.google.java.contract.Contracted} annotations. After a call to
   * this method, manual checks can still be performed with
   * {@link #assertLoadedClassesContracted()}.
   */
  public void assertLoadedClassesContracted()
      throws SpecificationError {
    ClassLoader loader = getClass().getClassLoader();
    BlacklistManager blacklistManager = BlacklistManager.getInstance();
    Iterator<String> iter = futureContractedChecks.iterator();
    while (iter.hasNext()) {
      String name = iter.next();
      try {
        assertContracted(Class.forName(name, false, loader));
      } catch (SpecificationError e) {
        /*
         * Rethrow SpecificationError from assertContracted so it
         * doesn't get caught by the catch-all filter below.
         */
        throw e;
      } catch (ClassNotFoundException e) {
        /* Ignore transient classes. */
      } catch (Throwable e) {
        /*
         * Ignore class loading artefacts.
         *
         * TODO(lenh): We should really only catch
         * ClassNotFoundException but this part seems to be highly
         * sensitive and has been reported to fail on runtime errors,
         * too.
         */
        DebugUtils.info("contractedcheck",
                        "while checking " + name + ": " + e.toString());
      }
      iter.remove();
    }
  }
}
