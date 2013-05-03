/*
 * Copyright 2013 Nhat Minh Lê
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
package com.google.java.contract.tests;

import com.google.java.contract.Ensures;
import com.google.java.contract.Invariant;
import com.google.java.contract.Requires;

/**
 * This class exposes some empty contracts that should be
 * ignored. Note also that not specifying the value is illegal for
 * a contract annotation, but is not enforced by OpenJDK javac when
 * using -proc:only; hence the warnings may differ between normal
 * compilation and contract compilation.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
@Invariant({})
class EmptyContracts {
  @Requires({})
  @Ensures({})
  void emptiness() {
  }
}
