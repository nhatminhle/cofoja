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
package com.google.java.contract.core.model;

/**
 * The variance of a kind of contract. Contracts are either
 * contravariant (preconditions), covariant (postconditions and
 * invariants), or have no variance (represented by {@code null}).
 *
 * <p>Multiple contravariant contracts should be combined using the
 * <em>or</em> combinator. Multiple covariant contracts should be
 * combined using the <em>and</em> combinator. Contracts that have no
 * variance should not be combined.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public enum ContractVariance {
  /**
   * A contravariant contract.
   */
  CONTRAVARIANT,

  /**
   * A covariant contract.
   */
  COVARIANT;
}
