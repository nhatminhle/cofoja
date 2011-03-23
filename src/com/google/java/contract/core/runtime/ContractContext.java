/*
 * Copyright 2007 Johannes Rieken
 * Copyright 2010 Google Inc.
 * Copyright 2011 Nhat Minh Lê
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

import java.util.IdentityHashMap;

/**
 * A helper to evaluate and enable method specifications. At runtime
 * it is the interface between instrumented bytecode and Contracts for
 * Java. Each thread has at most one context attached to it.
 *
 * <p>The ContractContext is responsible for:
 *
 * <ul>
 * <li>Disabling contract checking inside of contracts.
 * <li>Storage for failed predicate information.
 * </ul>
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 */
public class ContractContext {
  /**
   * The default size of the {@link #entered} object map.
   */
  private static final int ENTERED_DEFAULT_SIZE = 100;

  static ThreadLocal<ContractContext> context =
      new ThreadLocal<ContractContext>() {
    @Override
    protected ContractContext initialValue() {
      return new ContractContext();
    }
  };

  protected boolean busy;
  protected IdentityHashMap<Object, Void> entered;

  protected ContractContext() {
    entered = new IdentityHashMap<Object, Void>(ENTERED_DEFAULT_SIZE);
  }

  /**
   * Marks the start of a contract evaluation block.
   *
   * @return {@code true} if contracts should be evaluated,
   * {@code false} otherwise
   */
  public boolean tryEnterContract() {
    if (busy) {
      return false;
    } else {
      busy = true;
      return true;
    }
  }

  /**
   * Marks the start of a contract evaluation block.
   */
  public void leaveContract() {
    busy = false;
  }

  /**
   * Queries whether invariants should be checked for the current
   * method call. If it returns {@code true}, {@link #leave(Object)}
   * must be called on method exit.
   *
   * @return {@code true} if invariants should be evaluated,
   * {@code false} otherwise
   */
  public boolean tryEnter(Object obj) {
    if (entered.containsKey(obj)) {
      return false;
    } else {
      entered.put(obj, null);
      return true;
    }
  }

  /**
   * Must be called if and only if {@link #tryEnter(Object)}
   * previously returned {@code true} for this call frame.
   */
  public void leave(Object obj) {
    entered.remove(obj);
  }

  /**
   * Resets the busy state of this context.
   */
  public void clear() {
    busy = false;
  }
}
