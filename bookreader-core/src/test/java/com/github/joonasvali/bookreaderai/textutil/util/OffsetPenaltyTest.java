package com.github.joonasvali.bookreaderai.textutil.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OffsetPenaltyTest {

  // Tolerance for floating point comparisons.
  private static final float DELTA = 0.01f;

  @Test
  public void testGroup1() {
    OffsetPenalty offsetPenalty = new OffsetPenalty();
    assertEquals(0.10f, offsetPenalty.calculateOffsetPenalty(1, 1, 0.01f, 0.2f), DELTA);
    assertEquals(0.40f, offsetPenalty.calculateOffsetPenalty(1, 1, 0.99f, 0.99f), DELTA);
    assertEquals(0.79f,  offsetPenalty.calculateOffsetPenalty(2, 2, 0.99f, 0.99f), DELTA);
    assertEquals(0.88f, offsetPenalty.calculateOffsetPenalty(3, 3, 0.99f, 0.99f), DELTA);
  }

  @Test
  public void testGroup2() {
    OffsetPenalty offsetPenalty = new OffsetPenalty();
    assertEquals(0.23f, offsetPenalty.calculateOffsetPenalty(1, 1, 0.5f, 0.5f), DELTA);
    assertEquals(0.55f, offsetPenalty.calculateOffsetPenalty(2, 2, 0.5f, 0.5f), DELTA);
    assertEquals(0.66f, offsetPenalty.calculateOffsetPenalty(3, 3, 0.5f, 0.5f), DELTA);
    assertEquals(0.68f, offsetPenalty.calculateOffsetPenalty(4, 4, 0.5f, 0.5f), DELTA);
  }

  @Test
  public void testGroup3() {
    OffsetPenalty offsetPenalty = new OffsetPenalty();
    assertEquals(0.35f, offsetPenalty.calculateOffsetPenalty(50, 50, 0.1f, 0.1f), DELTA);
    assertEquals(0.88f,       offsetPenalty.calculateOffsetPenalty(50, 50, 0.5f, 0.5f), DELTA);
    assertEquals(0.98f,  offsetPenalty.calculateOffsetPenalty(50, 50, 0.99f, 0.99f), DELTA);
  }

  @Test
  public void testGroup4() {
    OffsetPenalty offsetPenalty = new OffsetPenalty();
    assertEquals(0.53f, offsetPenalty.calculateOffsetPenalty(10, 10, 0.3f, 0.3f), DELTA);
    assertEquals(0.72f,      offsetPenalty.calculateOffsetPenalty(10, 10, 0.5f, 0.5f), DELTA);
    assertEquals(0.83f, offsetPenalty.calculateOffsetPenalty(10, 10, 0.7f, 0.7f), DELTA);
  }
}
