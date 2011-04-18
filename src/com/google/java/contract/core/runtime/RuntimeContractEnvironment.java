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

import com.google.java.contract.ContractEnvironment;

/**
 * A contract environment running under the Cofoja Java agent.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class RuntimeContractEnvironment implements ContractEnvironment {
  protected BlacklistManager blacklistManager;

  public RuntimeContractEnvironment() {
    blacklistManager = BlacklistManager.getInstance();
  }

  @Override
  public void enablePreconditions(String pattern) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void disablePreconditions(String pattern) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void enablePostconditions(String pattern) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void disablePostconditions(String pattern) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void enableInvariants(String pattern) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void disableInvariants(String pattern) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasPreconditionsEnabled(Class<?> clazz) {
    return false;
  }

  @Override
  public boolean hasPreconditionsEnabled(String pattern) {
    return false;
  }

  @Override
  public boolean hasPostconditionsEnabled(Class<?> clazz) {
    return false;
  }

  @Override
  public boolean hasPostconditionsEnabled(String pattern) {
    return false;
  }

  @Override
  public boolean hasInvariantsEnabled(Class<?> clazz) {
    return false;
  }

  @Override
  public boolean hasInvariantsEnabled(String pattern) {
    return false;
  }

  @Override
  public void ignore(String pattern) {
    blacklistManager.ignore(pattern);
  }

  @Override
  public void unignore(String pattern) {
    blacklistManager.unignore(pattern);
  }

  @Override
  public boolean isIgnored(String pattern) {
    return blacklistManager.isIgnored(pattern);
  }
}
