package com.github.joonasvali.bookreaderai.textutil.util;

import org.junit.jupiter.api.Test;

public class OffsetPenaltyTest {
  @Test
  public void test() {
    OffsetPenalty offsetPenalty = new OffsetPenalty(0.65f);
    System.out.println(offsetPenalty.calculateOffsetPenalty(0, 0));
    System.out.println(offsetPenalty.calculateOffsetPenalty(0, 1));
    System.out.println(offsetPenalty.calculateOffsetPenalty(1, 0));
    System.out.println(offsetPenalty.calculateOffsetPenalty(1, 1));
    System.out.println(offsetPenalty.calculateOffsetPenalty(1, 2));
    System.out.println(offsetPenalty.calculateOffsetPenalty(2, 0));
    System.out.println(offsetPenalty.calculateOffsetPenalty(2, 1));
    System.out.println(offsetPenalty.calculateOffsetPenalty(2, 2));
    System.out.println(offsetPenalty.calculateOffsetPenalty(2, 3));
    System.out.println(offsetPenalty.calculateOffsetPenalty(3, 0));
    System.out.println(offsetPenalty.calculateOffsetPenalty(4, 0));
    System.out.println(offsetPenalty.calculateOffsetPenalty(3, 2));
    System.out.println(offsetPenalty.calculateOffsetPenalty(3, 3));
    System.out.println(offsetPenalty.calculateOffsetPenalty(4, 2));
    System.out.println(offsetPenalty.calculateOffsetPenalty(2, 4));
    System.out.println(offsetPenalty.calculateOffsetPenalty(4, 3));
    System.out.println(offsetPenalty.calculateOffsetPenalty(3, 4));
    System.out.println(offsetPenalty.calculateOffsetPenalty(4, 4));
    System.out.println(offsetPenalty.calculateOffsetPenalty(5, 4));
    System.out.println(offsetPenalty.calculateOffsetPenalty(4, 5));
    System.out.println(offsetPenalty.calculateOffsetPenalty(5, 5));
  }
}
