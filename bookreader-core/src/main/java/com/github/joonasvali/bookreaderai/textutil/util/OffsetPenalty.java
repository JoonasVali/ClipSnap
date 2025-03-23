package com.github.joonasvali.bookreaderai.textutil.util;

public class OffsetPenalty {
  private final float kPercentage;
  private final float k;

  public OffsetPenalty() {
    this(0.1f, 0.15f);
  }

  public OffsetPenalty(float scalingFactorPercentage, float scalingFactor) {
    kPercentage = (float) -Math.log(scalingFactorPercentage);
    k = (float) -Math.log(scalingFactor);
  }

  public float calculateOffsetPenalty(int offset1, int offset2, float offsetPercentage1, float offsetPercentage2) {
    float offsetPenaltyFromOffsetNumber = calculateOffsetPenalty(offset1, offset2);

    // Ensure values are within the expected range (0 to 1)
    offsetPercentage1 = Math.max(0.0f, Math.min(1.0f, offsetPercentage1));
    offsetPercentage2 = Math.max(0.0f, Math.min(1.0f, offsetPercentage2));

    // Compute total offset as a float percentage
    float offsetSum = offsetPercentage1 + offsetPercentage2;

    // Compute penalty: 1 - exp(-k * offsetSum)
    return (1 - (float) Math.exp(-k * offsetSum)) * offsetPenaltyFromOffsetNumber;
  }

  private float calculateOffsetPenalty(int offset1, int offset2) {
    // Calculate the total offset. If negative, clamp it to 0.
    int offsetSum = Math.max(0, offset1 + offset2);
    float penalty = 1 - (float) Math.exp(-k * offsetSum);
    // Ensure the value is between 0 and 1.
    return Math.min(1.0f, Math.max(0.0f, penalty));
  }
}