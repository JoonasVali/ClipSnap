package com.github.joonasvali.bookreaderai.imageutil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CutImageUtilTest {

  @Test
  public void testSplitImageIntoSections() {
    int width = 100;
    int height = 500;
    int verticalPieces = 3;
    int overlapPx = 10;

    // Create a test image with three horizontal bands:
    // Top band: red, middle band: green, bottom band: blue.
    BufferedImage testImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = testImage.createGraphics();

    // Fill top third with red.
    g2d.setColor(Color.RED);
    g2d.fillRect(0, 0, width, height / 3);

    // Fill middle third with green.
    g2d.setColor(Color.GREEN);
    g2d.fillRect(0, height / 3, width, height / 3);

    // Fill bottom third with blue.
    g2d.setColor(Color.BLUE);
    g2d.fillRect(0, 2 * height / 3, width, height - 2 * height / 3);

    g2d.dispose();

    // Cut the image using the method under test.
    CutImageUtil.SplitImageResult result = CutImageUtil.splitImageIntoSections(testImage, verticalPieces, overlapPx, false);
    BufferedImage[] slices = result.sections;
    assertFalse(result.useBrightOverlay);

    // Now verify that each slice contains the expected colors.
    // Note: With overlap, boundaries will include parts of adjacent color bands.

    // First slice: Should have red and a bit of green overlap.
    assertTrue(containsColor(slices[0], Color.RED), "First slice should contain red");
    assertFalse(containsColor(slices[0], Color.GREEN), "First slice should not contain green");
    assertFalse(containsColor(slices[0], Color.BLUE), "First slice should not contain blue");

    assertTrue(calculatePercentageOfColor(slices[0], Color.RED) > 0.85, "First slice should contain mostly red");

    // Second slice: Should have red overlap at the top, green in the middle, and blue overlap at the bottom.
    assertTrue(containsColor(slices[1], Color.RED), "Second slice should contain red overlap");
    assertTrue(containsColor(slices[1], Color.GREEN), "Second slice should contain green");
    assertFalse(containsColor(slices[1], Color.BLUE), "First slice should not contain blue");
    assertTrue(calculatePercentageOfColor(slices[1], Color.GREEN) > 0.85, "Second slice should contain mostly green");

    // Third slice: Should have green overlap at the top and blue in the bottom.
    assertFalse(containsColor(slices[2], Color.RED), "Third slice should not contain red");
    assertTrue(containsColor(slices[2], Color.GREEN), "Third slice should contain green overlap");
    assertTrue(containsColor(slices[2], Color.BLUE), "Third slice should contain blue");
    assertTrue(calculatePercentageOfColor(slices[2], Color.BLUE) > 0.85, "Third slice should contain mostly blue");
  }


  @Test
  public void testSplitImageIntoSectionsWithBrightOverlap() {
    int width = 100;
    int height = 500;
    int verticalPieces = 3;
    int overlapPx = 10;

    // Create a test image with three horizontal bands:
    // Top band: red, middle band: green, bottom band: blue.
    BufferedImage testImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = testImage.createGraphics();

    // Fill top third with red.
    g2d.setColor(Color.RED);
    g2d.fillRect(0, 0, width, height / 3);

    // Fill middle third with green.
    g2d.setColor(Color.GREEN);
    g2d.fillRect(0, height / 3, width, height / 3);

    // Fill bottom third with blue.
    g2d.setColor(Color.BLUE);
    g2d.fillRect(0, 2 * height / 3, width, height - 2 * height / 3);

    g2d.dispose();

    // Cut the image using the method under test.
    CutImageUtil.SplitImageResult result = CutImageUtil.splitImageIntoSections(testImage, verticalPieces, overlapPx, true);
    BufferedImage[] slices = result.sections;
    assertTrue(result.useBrightOverlay);

    // Now verify that each slice contains the expected colors.
    // Note: With overlap, boundaries will include parts of adjacent color bands.

    // First slice: Should have red and a bit of green overlap.
    assertFalse(containsColor(slices[0], Color.GREEN), "First slice should not contain green");
    assertFalse(containsColor(slices[0], Color.BLUE), "First slice should not contain blue");
    assertTrue(calculatePercentageOfColor(slices[0], Color.RED) > 0.85, "First slice should contain mostly red");

    // Second slice: Should have red overlap at the top, green in the middle, and blue overlap at the bottom.
    assertTrue(calculatePercentageOfColor(slices[1], Color.red) < 0.01 && calculatePercentageOfColor(slices[1], Color.red) > 0.001, "Third slice should not contain much red"); // very little red from the line
    assertTrue(calculatePercentageOfColor(slices[1], Color.GREEN) > 0.85, "Second slice should contain mostly green");

    // Third slice: Should have green overlap at the top and blue in the bottom.
    assertTrue(calculatePercentageOfColor(slices[1], Color.red) < 0.01 && calculatePercentageOfColor(slices[1], Color.red) > 0.001, "Third slice should not contain much red"); // very little red from the line
    assertTrue(calculatePercentageOfColor(slices[2], Color.BLUE) > 0.80, "Third slice should contain mostly blue");
  }

  @Test
  public void testSplitImageIntoSectionsWithDarkOverlap() {
    int width = 100;
    int height = 500;
    int verticalPieces = 3;
    int overlapPx = 10;

    // Create a test image with three horizontal bands:
    // Top band: red, middle band: green, bottom band: blue.
    BufferedImage testImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = testImage.createGraphics();

    // Fill top third with red.
    g2d.setColor(Color.lightGray);
    g2d.fillRect(0, 0, width, height / 3);

    // Fill middle third with green.
    g2d.setColor(Color.orange);
    g2d.fillRect(0, height / 3, width, height / 3);

    // Fill bottom third with blue.
    g2d.setColor(Color.pink);
    g2d.fillRect(0, 2 * height / 3, width, height - 2 * height / 3);

    g2d.dispose();

    // Cut the image using the method under test.
    CutImageUtil.SplitImageResult result = CutImageUtil.splitImageIntoSections(testImage, verticalPieces, overlapPx, true);
    BufferedImage[] slices = result.sections;
    assertFalse(result.useBrightOverlay);

    // Now verify that each slice contains the expected colors.
    // Note: With overlap, boundaries will include parts of adjacent color bands.

    // First slice: Should have red and a bit of green overlap.
    assertFalse(containsColor(slices[0], Color.orange), "First slice should not contain green");
    assertFalse(containsColor(slices[0], Color.pink), "First slice should not contain blue");
    assertTrue(calculatePercentageOfColor(slices[0], Color.lightGray) > 0.85, "First slice should contain mostly red");

    // Second slice: Should have red overlap at the top, green in the middle, and blue overlap at the bottom.
    assertTrue(calculatePercentageOfColor(slices[1], Color.red) < 0.01 && calculatePercentageOfColor(slices[1], Color.red) > 0.001, "Third slice should not contain much red"); // very little red from the line
    assertTrue(calculatePercentageOfColor(slices[1], Color.orange) > 0.85, "Second slice should contain mostly green");

    // Third slice: Should have green overlap at the top and blue in the bottom.
    assertTrue(calculatePercentageOfColor(slices[1], Color.red) < 0.01 && calculatePercentageOfColor(slices[1], Color.red) > 0.001, "Third slice should not contain much red"); // very little red from the line
    assertTrue(calculatePercentageOfColor(slices[2], Color.pink) > 0.80, "Third slice should contain mostly pink");
  }

  private float calculatePercentageOfColor(BufferedImage image, Color color) {
    int targetRGB = color.getRGB();
    int totalPixels = image.getWidth() * image.getHeight();
    int matchingPixels = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (image.getRGB(x, y) == targetRGB) {
          matchingPixels++;
        }
      }
    }
    return (float) matchingPixels / totalPixels;
  }

  // Helper method to check if a BufferedImage contains at least one pixel with the given color.
  private boolean containsColor(BufferedImage image, Color color) {
    int targetRGB = color.getRGB();
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (image.getRGB(x, y) == targetRGB) {
          return true;
        }
      }
    }
    return false;
  }


}
