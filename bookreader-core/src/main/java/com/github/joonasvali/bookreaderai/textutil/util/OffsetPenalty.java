package com.github.joonasvali.bookreaderai.textutil.util;

public class OffsetPenalty {
  private final float percentageModifier;
  private final float k;

  // Baseline weight for percentage contribution when absolute offset is low.
  private final float lowPercentageWeight = 0.12f;
  private final float beta = 0.005f;

  /**
   * Default constructor using percentageModifier=0.4 and scalingFactor=0.15.
   */
  public OffsetPenalty() {
    this(0.5f, 0.11f);
  }

  /**
   * @param percentageModifier controls how much the percentage penalty is amplified when the absolute offset is high.
   *                           For low absolute offsets (<=3) its effect is minimal.
   * @param scalingFactor        used to compute k = -ln(scalingFactor). Lower scalingFactor gives a steeper exponential curve.
   */
  public OffsetPenalty(float percentageModifier, float scalingFactor) {
    this.percentageModifier = percentageModifier;
    k = (float) -Math.log(scalingFactor);
  }

  /**
   * Calculate a penalty based on both absolute offsets and percentage offsets.
   * <p>
   * The percentage inputs (offsetPercentage1 and offsetPercentage2) are clamped to [0,1]. Their sum is then scaled
   * by an effective weight that is low (lowPercentageWeight) when the total absolute offset is small (<=3) and ramps
   * up toward percentageModifier for moderately high offsets. For very large absolute offsets, an additional linear
   * term (β × extra) is added to further boost the weight.
   *
   * @param offset1           the first absolute offset count
   * @param offset2           the second absolute offset count
   * @param offsetPercentage1 the first percentage offset (0 to 1)
   * @param offsetPercentage2 the second percentage offset (0 to 1)
   * @return a penalty value between 0 and 1
   */
  public float calculateOffsetPenalty(int offset1, int offset2, float offsetPercentage1, float offsetPercentage2) {
    // Clamp percentage offsets to [0,1]
    offsetPercentage1 = Math.max(0.0f, Math.min(1.0f, offsetPercentage1));
    offsetPercentage2 = Math.max(0.0f, Math.min(1.0f, offsetPercentage2));

    // Sum the percentage offsets.
    float percent = Math.max(offsetPercentage1, offsetPercentage2) * 2;
    int absoluteOffset = Math.max(offset1, offset2) * 2;
    // Compute the extra absolute offset beyond 3.
    float extra = Math.max(0, absoluteOffset - 3);

    // Compute an effective weight for the percentage contribution.
    // For extra==0, effective weight equals lowPercentageWeight.
    // As extra increases, it ramps up toward percentageModifier and then gets an additional boost from the beta term.
    float effectiveWeight = lowPercentageWeight
        + (percentageModifier - lowPercentageWeight) * (1 - (float) Math.exp(-extra))
        + beta * extra;

    // Multiply the percentage sum by this effective weight.
    float effectivePercentage = percent * effectiveWeight;

    // Compute the overall penalty using an exponential curve.
    return 1 - (float) Math.exp(-k * effectivePercentage);
  }
}
