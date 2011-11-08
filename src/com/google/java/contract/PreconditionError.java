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
 * An exception thrown when a precondition is violated.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @author johannes.rieken@gmail.com (Johannes Rieken)
 * @see com.google.java.contract.Requires
 */
public class PreconditionError extends ContractAssertionError {
  /**
   * Constructs a new PreconditionError.
   *
   * @param msg the error message.
   * @param cause a previously failing precondition
   */
  public PreconditionError(String msg, PreconditionError cause) {
    super(msg, cause, null);
  }

  /**
   * Constructs a new PreconditionError.
   *
   * @param msg the error message.
   * @param cause a previously failing precondition
   * @param throwable the throwable caught while evaluating contracts, or null
   *        for none.
   */
  public PreconditionError(String msg, PreconditionError cause, Throwable throwable) {
    super(msg, cause, throwable);
  }

  @Override
  protected String getMethodName(String contractedName) {
    return contractedName + ".<pre>";
  }
}
