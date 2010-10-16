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

import com.google.java.contract.Invariant;
import com.google.java.contract.core.runtime.RuntimeContractEnvironment;

/**
 * A contract environment running under the Cofoja Java agent.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant("activationManager != null")
public class AgentContractEnvironment extends RuntimeContractEnvironment {
  protected ActivationRuleManager activationManager;

  public AgentContractEnvironment() {
    activationManager = ActivationRuleManager.getInstance();
  }

  @Override
  public void enablePreconditions(String pattern) {
    activationManager.enablePreconditions(pattern);
  }

  @Override
  public void disablePreconditions(String pattern) {
    activationManager.disablePreconditions(pattern);
  }

  @Override
  public void enablePostconditions(String pattern) {
    activationManager.enablePostconditions(pattern);
  }

  @Override
  public void disablePostconditions(String pattern) {
    activationManager.disablePostconditions(pattern);
  }

  @Override
  public void enableInvariants(String pattern) {
    activationManager.enableInvariants(pattern);
  }

  @Override
  public void disableInvariants(String pattern) {
    activationManager.disableInvariants(pattern);
  }

  @Override
  public boolean hasPreconditionsEnabled(Class<?> clazz) {
    return activationManager.hasPreconditionsEnabled(clazz.getName());
  }

  @Override
  public boolean hasPreconditionsEnabled(String pattern) {
    return activationManager.hasPreconditionsEnabled(pattern);
  }

  @Override
  public boolean hasPostconditionsEnabled(Class<?> clazz) {
    return activationManager.hasPostconditionsEnabled(clazz.getName());
  }

  @Override
  public boolean hasPostconditionsEnabled(String pattern) {
    return activationManager.hasPostconditionsEnabled(pattern);
  }

  @Override
  public boolean hasInvariantsEnabled(Class<?> clazz) {
    return activationManager.hasInvariantsEnabled(clazz.getName());
  }

  @Override
  public boolean hasInvariantsEnabled(String pattern) {
    return activationManager.hasInvariantsEnabled(pattern);
  }
}
