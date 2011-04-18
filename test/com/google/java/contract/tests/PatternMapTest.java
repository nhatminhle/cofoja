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

import com.google.java.contract.core.util.PatternMap;

import junit.framework.TestCase;

/**
 * Unit test for {@link PatternMap}.
 *
 * @author nhat.minh.le@huoc.org (Nhat Minh Lê)
 */
public class PatternMapTest extends TestCase {
  private PatternMap<Integer> map;

  @Override
  protected void setUp() {
    map = new PatternMap<Integer>();
  }

  public void testExactMatch() {
    map.put("a.b.c.X", 0);
    map.put("a.b.Y", 1);
    map.put("a.b.c.d.Z", 2);
    map.put("a.b.Y1", 3);
    assertEquals(2, (int) map.get("a.b.c.d.Z"));
    map.put("a.b.Y2", 4);
    map.put("a.b.c.d.Z1", 5);
    assertEquals(1, (int) map.get("a.b.Y"));
    assertEquals(4, (int) map.get("a.b.Y2"));
  }

  public void testStarMatch() {
    map.put("a.b.c.*", 0);
    map.put("a.b.Y", 1);
    map.put("a.b.c.d.Z", 2);
    map.put("a.b.Y1", 3);
    assertEquals(0, (int) map.get("a.b.c.X"));
    assertEquals(2, (int) map.get("a.b.c.d.Z"));
    map.put("a.b.Y2", 4);
    map.put("a.b.c.d.Z1", 5);
    assertEquals(0, (int) map.get("a.b.c.U"));
    assertEquals(4, (int) map.get("a.b.Y2"));
  }

  public void testStarOverride() {
    map.put("a.b.Y", 1);
    map.put("a.b.c.d.Z", 2);
    map.put("a.b.c.*", 0);
    map.put("a.b.Y1", 3);
    assertEquals(0, (int) map.get("a.b.c.X"));
    assertEquals(0, (int) map.get("a.b.c.d.Z"));
    map.put("a.b.Y2", 4);
    map.put("a.b.c.d.Z1", 5);
    assertEquals(0, (int) map.get("a.b.c.U"));
    assertEquals(4, (int) map.get("a.b.Y2"));
  }

  public void testRedundantOverride() {
    map.put("a.*", 0);
    map.put("a.x.*", 1);
    map.put("a.x.u.*", 2);
    map.put("a.y.*", 0);
    map.put("a.y.u.*", 0);
    assertEquals(1, (int) map.get("a.x.X"));
    assertEquals(0, (int) map.get("a.y.X"));
    assertEquals(0, (int) map.get("a.a.X"));
    assertEquals(false, map.isOverriden("a.x.u.X"));
    assertEquals(false, map.isOverriden("a.x.u.*"));
    assertEquals(true, map.isOverriden("a.*"));
    assertEquals(true, map.isOverriden("a.x.*"));
    assertEquals(false, map.isOverriden("a.y.*"));
  }
}
