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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies postconditions that apply to the annotated method. The
 * annotated method must establish its postconditions if and only if
 * the preconditions were satisfied.
 *
 * <p>When run time checking of contracts is enabled, postconditions
 * are checked at method exit, when the method exits normally, of the
 * and throw a {@link com.google.java.contract.PreconditionError} when they
 * are violated. Postconditions are not checked when the method exits
 * by throwing an exception.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 * @see ThrowEnsures
 */
@Documented
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface Ensures {
  /**
   * The list of postconditions that must be met by the annotated
   * method, written as strings. The expressions must be valid Java
   * code and can reference all things visible to the class, as well
   * as the method's arguments. Since postconditions may need to refer
   * to the old value of an expression and the value returned by the
   * annotated method, the following extensions are allowed:
   *
   * <p>The keyword {@code result} refers to the value returned from
   * the method, if any. It is an error to have a method parameter
   * named {@code result}.
   *
   * <p>The {@code old(expression)} pseudo-method construct refers to
   * the value of its argument before execution of the method.
   * {@code expression} must be a balanced expression, with regard to
   * parentheses. The {@code old()} construct is preprocessed using
   * the following simple rules (similar to macro expansion done by a
   * C preprocessor):
   *
   * <ul>
   * <li>the word {@code old}, followed by an opening parenthesis, is
   * recognized as an identifier;
   * <li>the enclosed expression is only parsed for balanced
   * parentheses;
   * <li>textual substitution is used.
   * </ul>
   *
   * <p>It is an error to call a method named {@code old} from within
   * a postcondition.
   */
  String[] value();
}
