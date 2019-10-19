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
package com.google.java.contract.util;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Utilities for predicates.
 *
 * <p>These methods are intentionally name-compatible with
 * com.google.common.base.Predicates, so as to make it easy
 * to switch between them.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public final class Predicates {
  private static final Predicate<Object> TRUE = obj -> true;

  private static final Predicate<Object> FALSE = obj -> false;

  private static final Predicate<Object> IS_NULL = java.util.Objects::isNull;

  private static final Predicate<Object> NON_NULL = java.util.Objects::nonNull;

  private Predicates() {
  }

  /**
   * Narrows {@code p} to apply to type {@code T}.
   */
  @SuppressWarnings("unchecked")
  public static <S, T extends S> Predicate<T> narrow(Predicate<S> p) {
    return (Predicate<T>) p;
  }

  /**
   * Returns the constant predicate that always returns {@code b}.
   */
  public static <T> Predicate<T> constant(final boolean b) {
    return b
        ? Predicates.narrow(TRUE)
        : Predicates.narrow(FALSE);
  }

  /**
   * Returns a predicate that evaluates to {@code true} if its
   * argument is equal to {@code obj}.
   */
  public static <T> Predicate<T> equalTo(final T obj) {
    return o -> Objects.equals(o, obj);
  }

  /**
   * Returns a predicate that evaluates to {@code true} if its
   * argument is {@code null}.
   */
  public static <T> Predicate<T> isNull() {
    return Predicates.narrow(IS_NULL);
  }

  /**
   * Returns a predicate that evaluates to {@code true} if its
   * argument is not {@code null}.
   */
  public static <T> Predicate<T> nonNull() {
    return Predicates.narrow(NON_NULL);
  }

  /**
   * Returns a predicate that evaluates to {@code true} if its
   * argument is between {@code low} (inclusive) and {@code high}
   * (exclusive).
   */
  public static <T extends Comparable<T>> Predicate<T> between(
      final T low, final T high) {
    return obj -> obj != null
        && (low == null || obj.compareTo(low) >= 0)
        && (high == null || obj.compareTo(high) < 0);
  }

  /**
   * Returns a predicate that evaluates to {@code true} if its
   * argument is a member of {@code it}.
   */
  public static <T> Predicate<T> in(final Iterable<T> it) {
    return obj -> {
      for (T elem : it) {
        if (Objects.equals(elem, obj)) {
          return true;
        }
      }
      return false;
    };
  }

  /**
   * Returns a predicate that evaluates to {@code true} if its
   * argument is a member of {@code c}.
   */
  public static <T> Predicate<T> in(final Collection<T> c) {
    return c::contains;
  }

  /**
   * Returns a predicate that evaluates to {@code true} if its
   * argument does not satisfy {@code p}.
   */
  public static <T> Predicate<T> not(final Predicate<? super T> p) {
    return obj -> !p.test(obj);
  }

  /**
   * Returns a predicate that evaluates to {@code true} if its
   * argument satisfies all predicates {@code ps}.
   */
  @SafeVarargs
  public static <T> Predicate<T> and(final Predicate<? super T>... ps) {
    return obj -> {
      for (Predicate<? super T> p : ps) {
        if (!p.test(obj)) {
          return false;
        }
      }
      return true;
    };
  }

  /**
   * Returns a predicate that evaluates to {@code true} if its
   * argument satisfies any of the predicates {@code ps}.
   */
  @SafeVarargs
  public static <T> Predicate<T> or(final Predicate<? super T>... ps) {
    return obj -> {
      for (Predicate<? super T> p : ps) {
        if (p.test(obj)) {
          return true;
        }
      }
      return false;
    };
  }

  /**
   * Returns a predicate that evaluates to {@code true} if all the
   * elements of its argument satisfies {@code p}.
   */
  public static <T> Predicate<Iterable<T>> all(
      final Predicate<? super T> p) {
    return obj -> Iterables.all(obj, p);
  }

  /**
   * Returns a predicate that evaluates to {@code true} if any element
   * of its argument satisfies {@code p}.
   */
  public static <T> Predicate<Iterable<T>> any(
      final Predicate<? super T> p) {
    return obj -> Iterables.any(obj, p);
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the entry
   * set of its argument satisfies {@code p}.
   */
  public static <K, V> Predicate<Map<K, V>> forEntries(
      final Predicate<? super Set<Map.Entry<K, V>>> p) {
    return obj -> p.test(obj.entrySet());
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the key set
   * of its argument satisfies {@code p}.
   */
  public static <K, V> Predicate<Map<K, V>> forKeys(
      final Predicate<? super Set<K>> p) {
    return obj -> p.test(obj.keySet());
  }

  /**
   * Returns a predicate that evaluates to {@code true} if the value
   * collection of its argument satisfies {@code p}.
   */
  public static <K, V> Predicate<Map<K, V>> forValues(
      final Predicate<? super Collection<V>> p) {
    return obj -> p.test(obj.values());
  }

  /**
   * Returns a predicate that applies {@code any(p)} to the entries of
   * its argument.
   */
  public static <K, V> Predicate<Map<K, V>> anyEntry(
      Predicate<? super Map.Entry<K, V>> p) {
    return forEntries(Predicates.any(p));
  }

  /**
   * Returns a predicate that applies {@code any(p)} to the keys of
   * its argument.
   */
  public static <K, V> Predicate<Map<K, V>> anyKey(Predicate<? super K> p) {
    return forKeys(Predicates.any(p));
  }

  /**
   * Returns a predicate that applies {@code any(p)} to the values of
   * its argument.
   */
  public static <K, V> Predicate<Map<K, V>> anyValue(Predicate<? super V> p) {
    return forValues(Predicates.any(p));
  }

  /**
   * Returns a predicate that applies {@code all(p)} to the entries of
   * its argument.
   */
  public static <K, V> Predicate<Map<K, V>> allEntries(
      Predicate<? super Map.Entry<K, V>> p) {
    return forEntries(Predicates.all(p));
  }

  /**
   * Returns a predicate that applies {@code all(p)} to the keys of
   * its argument.
   */
  public static <K, V> Predicate<Map<K, V>> allKeys(Predicate<? super K> p) {
    return forKeys(Predicates.all(p));
  }

  /**
   * Returns a predicate that applies {@code all(p)} to the values of
   * its argument.
   */
  public static <K, V> Predicate<Map<K, V>> allValues(Predicate<? super V> p) {
    return forValues(Predicates.all(p));
  }
}
