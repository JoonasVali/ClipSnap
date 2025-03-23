package com.github.joonasvali.bookreaderai.textutil.util;

public class OffsetPenalty {
  private final float k;

  public OffsetPenalty() {
    this(0.75f);
  }

  /**
   * Create an OffsetPenalty object with a custom scaling factor.
   * @param scalingFactor the higher the value the less the penalty
   */
  public OffsetPenalty(float scalingFactor) {
    k = (float) -Math.log(scalingFactor);
  }

  public float calculateOffsetPenalty(int offset1, int offset2) {
    // Calculate the total offset. If negative, clamp it to 0.
    int offsetSum = Math.max(0, offset1 + offset2);
    // Compute penalty: 1 - exp(-k * offsetSum)
    float penalty = 1 - (float)Math.exp(-k * offsetSum);
    // Ensure the value is between 0 and 1.
    return Math.min(1.0f, Math.max(0.0f, penalty));
  }
}