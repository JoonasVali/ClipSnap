package com.github.joonasvali.bookreaderai.textutil.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OffsetPenaltyTest {

  // Tolerance for floating point comparisons.
  private static final float DELTA = 1e-6f;

  @Test
  public void testGroup1() {
    OffsetPenalty offsetPenalty = new OffsetPenalty();
    assertEquals(0.32120982f, offsetPenalty.calculateOffsetPenalty(1, 1, 0.01f, 0.2f), DELTA);
    assertEquals(0.95465577f, offsetPenalty.calculateOffsetPenalty(1, 1, 0.99f, 0.99f), DELTA);
    assertEquals(0.9761355f,  offsetPenalty.calculateOffsetPenalty(2, 2, 0.99f, 0.99f), DELTA);
    assertEquals(0.97661877f, offsetPenalty.calculateOffsetPenalty(3, 3, 0.99f, 0.99f), DELTA);
  }

  @Test
  public void testGroup2() {
    OffsetPenalty offsetPenalty = new OffsetPenalty();
    assertEquals(0.83087504f, offsetPenalty.calculateOffsetPenalty(1, 1, 0.5f, 0.5f), DELTA);
    assertEquals(0.84956974f, offsetPenalty.calculateOffsetPenalty(2, 2, 0.5f, 0.5f), DELTA);
    assertEquals(0.84999037f, offsetPenalty.calculateOffsetPenalty(3, 3, 0.5f, 0.5f), DELTA);
  }

  @Test
  public void testGroup3() {
    OffsetPenalty offsetPenalty = new OffsetPenalty();
    assertEquals(0.3286034f, offsetPenalty.calculateOffsetPenalty(50, 50, 0.01f, 0.2f), DELTA);
    assertEquals(0.85f,       offsetPenalty.calculateOffsetPenalty(50, 50, 0.5f, 0.5f), DELTA);
    assertEquals(0.9766299f,  offsetPenalty.calculateOffsetPenalty(50, 50, 0.99f, 0.99f), DELTA);
  }

  @Test
  public void testGroup4() {
    OffsetPenalty offsetPenalty = new OffsetPenalty();
    assertEquals(0.6796279f, offsetPenalty.calculateOffsetPenalty(10, 10, 0.3f, 0.3f), DELTA);
    assertEquals(0.85f,      offsetPenalty.calculateOffsetPenalty(10, 10, 0.5f, 0.5f), DELTA);
    assertEquals(0.92976916f, offsetPenalty.calculateOffsetPenalty(10, 10, 0.7f, 0.7f), DELTA);
  }
}
