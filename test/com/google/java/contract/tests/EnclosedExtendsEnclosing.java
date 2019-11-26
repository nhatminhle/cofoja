/*
 * Copyright 2011 Google Inc.
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

import com.google.java.contract.Invariant;

/**
 * A simple enclosed class that extends the enclosing one and is contracted.
 *
 * @author chatain@google.com (Leonardo Chatain)
 */
public class EnclosedExtendsEnclosing {
  @Invariant("field != 0")
  static class Enclosed extends EnclosedExtendsEnclosing {
    private final int field;
    Enclosed(int field) {
      this.field = field;
    }
  }
}
