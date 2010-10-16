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
package com.google.java.contract.core.runtime;

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
  protected static ThreadLocal<ContractContext> context =
      new ThreadLocal<ContractContext>() {
    @Override
    protected ContractContext initialValue() {
      return new ContractContext();
    }
  };

  protected boolean busy;

  /**
   * Retrieves the contract context associated with the specified
   * thread.
   */
  static ContractContext getContractContext() {
    return context.get();
  }

  protected ContractContext() {
    busy = false;
  }

  /**
   * Begins evaluation of an assertion block.
   *
   * @return {@code true} if the contract should be evaluated, false
   * otherwise
   */
  public boolean enter() {
    if (busy) {
      return false;
    } else {
      busy = true;
      return true;
    }
  }

  /**
   * Ends evaluation of an assertion block.
   */
  public void leave() {
    busy = false;
  }
}
