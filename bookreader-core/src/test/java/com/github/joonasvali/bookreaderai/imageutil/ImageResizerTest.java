package com.github.joonasvali.bookreaderai.imageutil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageResizerTest {

  /**
   * Provided example: a tall image is scaled down so that its dimensions become 500x1000,
   * and its aspect ratio is preserved.
   */
  @Test
  public void testScalingDown() {
    ImageResizer imageResizer = new ImageResizer(500, 1000, 100);
    BufferedImage image = new BufferedImage(1000, 2000, BufferedImage.TYPE_INT_RGB);
    double originalAspectRatio = (double) image.getWidth() / image.getHeight();
    BufferedImage result = imageResizer.resizeImageToLimits(image);

    Assertions.assertEquals(1000, result.getHeight());
    Assertions.assertEquals(500, result.getWidth());
    Assertions.assertEquals(originalAspectRatio, (double) result.getWidth() / result.getHeight(), 0.0001);
  }

  /**
   * When the image already meets both the maximum and minimum constraints,
   * no resizing should occur.
   */
  @Test
  public void testNoResizingNeeded() {
    // Image dimensions 300x400 already satisfy: maxShort >= 300, maxLong >= 400, and both >= minShort (100).
    ImageResizer imageResizer = new ImageResizer(500, 1000, 100);
    BufferedImage image = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);
    BufferedImage result = imageResizer.resizeImageToLimits(image);

    // The dimensions should remain unchanged.
    Assertions.assertEquals(300, result.getWidth());
    Assertions.assertEquals(400, result.getHeight());
  }

  /**
   * For an image that is too small, a uniform scaling up should occur when it does not cause
   * any dimension to exceed the maximum limits.
   */
  @Test
  public void testScalingUpWithoutPadding() {
    // Use parameters such that the image is within max limits but below min limits.
    // Image: 100x150, minShortSize = 200.
    // After max limits, image remains 100x150.
    // In min limits: scaleFactor = max(200/100, 200/150) = 2, so result should be 200x300.
    ImageResizer imageResizer = new ImageResizer(500, 1000, 200);
    BufferedImage image = new BufferedImage(100, 150, BufferedImage.TYPE_INT_RGB);
    BufferedImage result = imageResizer.resizeImageToLimits(image);

    Assertions.assertEquals(200, result.getWidth());
    Assertions.assertEquals(300, result.getHeight());

    // Check that aspect ratio is preserved.
    double originalAspect = (double) image.getWidth() / image.getHeight();
    double resultAspect = (double) result.getWidth() / result.getHeight();
    Assertions.assertEquals(originalAspect, resultAspect, 0.0001);
  }

  /**
   * When a landscape image is too small in one dimension such that scaling up would
   * exceed max limits, the algorithm should pad the image instead.
   *
   * Example:
   * - Original image: 500x100.
   * - Resizer parameters: maxShort=200, maxLong=300, minShort=150.
   *   -> In resizeImageToMaxLimits: scales image to 300x60.
   *   -> In resizeImageToMinLimits: scaling would yield 750x150 (exceeds maxLong), so padding is applied.
   *   -> Final image: 300x150 with the 300x60 image centered vertically.
   */
  @Test
  public void testPaddingForScalingUpExceedsMaxLandscape() {
    ImageResizer imageResizer = new ImageResizer(200, 300, 150);
    BufferedImage image = new BufferedImage(500, 100, BufferedImage.TYPE_INT_RGB);
    BufferedImage result = imageResizer.resizeImageToLimits(image);

    // Final dimensions should be padded to 300x150.
    Assertions.assertEquals(300, result.getWidth());
    Assertions.assertEquals(150, result.getHeight());
  }

  @Test
  public void testPaddingForScalingUpExceedsMaxLandscapeWithColorCheck() {
    ImageResizer imageResizer = new ImageResizer(200, 300, 150);

    // Create a white image of size 500x100.
    BufferedImage image = new BufferedImage(500, 100, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();
    g2d.setColor(Color.WHITE);
    g2d.fillRect(0, 0, 500, 100);
    g2d.dispose();

    BufferedImage result = imageResizer.resizeImageToLimits(image);

    // Final dimensions should be padded to 300x150.
    Assertions.assertEquals(300, result.getWidth());
    Assertions.assertEquals(150, result.getHeight());

    // After resizeImageToMaxLimits, the image becomes 300x60.
    // In padding, the image is centered vertically: top and bottom margins should be (150 - 60) / 2 = 45 pixels each.

    // Check that a pixel in the top padding area is black.
    int topPaddingRGB = result.getRGB(150, 20);
    Assertions.assertEquals(Color.BLACK.getRGB(), topPaddingRGB, "Top padding should be black.");

    // Check that a pixel in the bottom padding area is black.
    int bottomPaddingRGB = result.getRGB(150, 140);
    Assertions.assertEquals(Color.BLACK.getRGB(), bottomPaddingRGB, "Bottom padding should be black.");

    // Check that a pixel within the central area (the scaled image) is white.
    int centralRGB = result.getRGB(150, 75);
    Assertions.assertEquals(Color.WHITE.getRGB(), centralRGB, "Central image area should be white.");
  }


  /**
   * When a portrait image is too narrow, scaling up uniformly would exceed max limits.
   * In this case, padding should be applied.
   *
   * Example:
   * - Original image: 100x400.
   * - Resizer parameters: maxShort=300, maxLong=500, minShort=150.
   *   -> In max limits: image remains 100x400 (within max limits).
   *   -> In min limits: scaling factor would be 1.5 (yielding 150x600, but 600 > maxLong 500),
   *      so padding is applied.
   *   -> Final image: 150x400 with the original image centered horizontally.
   */
  @Test
  public void testPaddingForScalingUpExceedsMaxPortrait() {
    ImageResizer imageResizer = new ImageResizer(300, 500, 150);
    BufferedImage image = new BufferedImage(100, 400, BufferedImage.TYPE_INT_RGB);
    BufferedImage result = imageResizer.resizeImageToLimits(image);

    // Final dimensions: width padded to 150, height remains 400.
    Assertions.assertEquals(150, result.getWidth());
    Assertions.assertEquals(400, result.getHeight());
  }

  /**
   * For a square image below the minimum limits, scaling up should be applied if it does not cause
   * any of the maximum constraints to be violated.
   */
  @Test
  public void testSquareScalingUp() {
    // Image: 100x100, minShortSize = 200.
    // In max limits: image remains 100x100.
    // In min limits: scaling factor = 2 (since 200/100 = 2) resulting in 200x200.
    ImageResizer imageResizer = new ImageResizer(500, 1000, 200);
    BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
    BufferedImage result = imageResizer.resizeImageToLimits(image);

    Assertions.assertEquals(200, result.getWidth());
    Assertions.assertEquals(200, result.getHeight());
  }
}

