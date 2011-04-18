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
package com.google.java.contract.tests;

import com.google.java.contract.InvariantError;
import com.google.java.contract.PostconditionError;
import com.google.java.contract.PreconditionError;
import com.google.java.contract.examples.ArrayListStack;
import com.google.java.contract.examples.Stack;

import junit.framework.TestCase;

/**
 * Tests implementations of {@link Stack} against the contracts
 * specified in the interface.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class StackTest extends TestCase {
  /**
   * A stack implementation that does not preserve its invariant.
   */
  public static class BogusInvariantStack<T> implements Stack<T> {
    @Override
    public int size() {
      return -1;
    }

    @Override
    public T peek() {
      return null;
    }

    @Override
    public T pop() {
      return null;
    }

    @Override
    public void push(T obj) {
    }
  }

  /**
   * A stack implementation that does not preserve its postconditions.
   */
  public static class BogusPostconditionsStack<T>
      extends BogusInvariantStack<T> {
    @Override
    public int size() {
      return 0;
    }
  }

  /**
   * A stack implementation that simply counts its elements instead of
   * storing them.
   */
  public static class BogusCountingStack<T>
      extends BogusPostconditionsStack<T> {
    protected int count;

    public BogusCountingStack() {
      count = 0;
    }

    @Override
    public int size() {
      return count;
    }

    @Override
    public T pop() {
      --count;
      return null;
    }

    @Override
    public void push(T obj) {
      ++count;
    }
  }

  /**
   * A stack implementation that simply stores the last element. This
   * implementation suffices to fool our contracts.
   */
  public static class BogusLastElementStack<T>
      extends BogusCountingStack<T> {
    protected T lastElement;

    @Override
    public T peek() {
      return lastElement;
    }

    @Override
    public T pop() {
      --count;
      return lastElement;
    }

    @Override
    public void push(T obj) {
      ++count;
      lastElement = obj;
    }
  }

  private Stack<Integer> stack;

  @Override
  public void setUp() {
    stack = new ArrayListStack<Integer>();
  }

  public void testNormal() {
    stack.push(1);
    stack.push(2);
    assertEquals(2, stack.size());
    stack.pop();
    stack.push(3);
    stack.pop();
    stack.pop();
    stack.push(4);
    assertEquals(1, stack.size());
  }

  public void testEmptyPop() {
    try {
      stack.pop();
      fail();
    } catch (PreconditionError expected) {
      assertEquals("[size() >= 1]", expected.getMessages().toString());
    }
  }

  public void testBogusInvariant() {
    try {
      Stack<Integer> bogusInvariantStack = new BogusInvariantStack<Integer>();
      fail();
    } catch (InvariantError expected) {
      assertEquals("[size() >= 0]", expected.getMessages().toString());
    }
  }

  public void testBogusPostPush() {
    Stack<Integer> bogusPostconditionsStack =
        new BogusPostconditionsStack<Integer>();
    try {
      bogusPostconditionsStack.push(1);
      fail();
    } catch (PostconditionError expected) {
      assertEquals("[size() == old (size()) + 1]",
                   expected.getMessages().toString());
    }
  }

  public void testBogusPostPush1() {
    Stack<Integer> countingStack = new BogusCountingStack<Integer>();
    try {
      countingStack.push(1);
      fail();
    } catch (PostconditionError expected) {
      assertEquals("[peek() == old (obj)]", expected.getMessages().toString());
    }
  }

  public void testIneffectiveBogusPostPush() {
    Stack<Integer> lastElementStack = new BogusLastElementStack<Integer>();
    lastElementStack.push(1);
    lastElementStack.push(2);
    lastElementStack.pop();
  }
}
