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
package com.google.java.contract;

/**
 * An exception thrown when a class invariant is violated.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 * @see com.google.java.contract.Invariant
 */
public class InvariantError extends ContractAssertionError {
  /**
   * Constructs a new InvariantError.
   *
   * @param msg the error message.
   */
  public InvariantError(String msg) {
    super(msg, null);
  }

  /**
   * Constructs a new InvariantError.
   *
   * @param msg the error message.
   * @param throwable the throwable caught while evaluating contracts, or null
   *        for none.
   */
  public InvariantError(String msg, Throwable throwable) {
    super(msg, throwable);
  }

  @Override
  protected String getMethodName(String contractedName) {
    return "<invariant>";
  }
}
