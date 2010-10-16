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
package com.google.java.contract;

/**
 * An object that can be called by Contracts for Java to configure the
 * contract environment.
 *
 * <p>As the last step of its startup procedure, Contracts for Java
 * instantiates a new object of the class specified by the system
 * property {@code com.google.java.contract.configurator} and calls the method
 * {@link #configure(ContractEnvironment)} on it.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public interface ContractEnvironmentConfigurator {
  /**
   * Configures the contract environment. It is safe for this method
   * to store {@code contractEnv} for later use, if needed.
   */
  @Requires("contractEnv != null")
  public void configure(ContractEnvironment contractEnv);
}
