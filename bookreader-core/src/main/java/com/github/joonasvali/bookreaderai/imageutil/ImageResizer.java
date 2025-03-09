package com.github.joonasvali.bookreaderai.imageutil;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageResizer {
  private final int maxShortSize;
  private final int maxLongSize;
  private final int minShortSize;

  public ImageResizer(int maxShortSize, int maxLongSize, int minShortSize) {
    this.maxShortSize = maxShortSize;
    this.maxLongSize = maxLongSize;
    this.minShortSize = minShortSize;
  }

  public static ImageResizer getStandardOpenAIImageResizer() {
    // https://platform.openai.com/docs/guides/vision
    return new ImageResizer(768, 2000, 500);
  }

  public BufferedImage resizeImageToLimits(BufferedImage input) {
    BufferedImage resized = resizeImageToMaxLimits(input);
    return resizeImageToMinLimits(resized);
  }

  /**
   * Rescales the given BufferedImage so that:
   * - The shorter side is less than {maxShortSize} px, and
   * - The longer side is less than {maxLongSize} px.
   *
   * The aspect ratio is preserved and the image is scaled down only if necessary.
   *
   * @param input the original BufferedImage
   * @return a resized BufferedImage meeting the guidelines, or the original image if resizing is not needed.
   */
  public BufferedImage resizeImageToMaxLimits(BufferedImage input) {
    int width = input.getWidth();
    int height = input.getHeight();

    // Identify the short and long sides.
    int shortSide = Math.min(width, height);
    int longSide = Math.max(width, height);

    // Compute scaling factors for both dimensions.
    // Note: If the image is smaller than the limits, these factors will be > 1.
    double scaleForShort = (double) maxShortSize / shortSide;
    double scaleForLong = (double) maxLongSize / longSide;

    // We only want to scale down (not upscale), so limit the scale factor to 1.
    double scaleFactor = Math.min(1.0, Math.min(scaleForShort, scaleForLong));

    // If scaling is not needed, return the original image.
    if (scaleFactor >= 1.0) {
      return input;
    }

    // Calculate new dimensions.
    int newWidth = (int) Math.round(width * scaleFactor);
    int newHeight = (int) Math.round(height * scaleFactor);

    // Create a new BufferedImage with the new dimensions.
    BufferedImage resized = new BufferedImage(newWidth, newHeight, input.getType());
    Graphics2D g2d = resized.createGraphics();

    // Use high quality scaling.
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g2d.drawImage(input, 0, 0, newWidth, newHeight, null);
    g2d.dispose();

    return resized;
  }

  /**
   * Scales the given BufferedImage so that neither width nor height is less than {minShortSize} pixels.
   * The aspect ratio is preserved. If the image already meets the minimum size requirements,
   * the original image is returned. In case the resizing can't be done due to maximum dimensions being
   * exceeded, a black padding is added to meet the minimum size requirements.
   *
   * @param input the original BufferedImage
   * @return a BufferedImage with both dimensions at least {minShortSize}px.
   */
  public BufferedImage resizeImageToMinLimits(BufferedImage input) {
    int width = input.getWidth();
    int height = input.getHeight();

    // If both dimensions already meet the minimum, no action is needed.
    if (width >= minShortSize && height >= minShortSize) {
      return input;
    }

    // Compute the scale factors needed to bring each dimension to minShortSize.
    double scaleFactorWidth = (double) minShortSize / width;
    double scaleFactorHeight = (double) minShortSize / height;
    // Use the larger factor so that both dimensions meet the min requirement.
    double scaleFactor = Math.max(scaleFactorWidth, scaleFactorHeight);

    int potentialWidth = (int) Math.round(width * scaleFactor);
    int potentialHeight = (int) Math.round(height * scaleFactor);

    // Determine if scaling would exceed the max limits.
    // For a portrait image (width < height), width is the short side and height is the long side.
    // For landscape (or square), height is the short side.
    boolean scalingExceedsMax = false;
    if (width < height) { // Portrait
      if (potentialWidth > maxShortSize || potentialHeight > maxLongSize) {
        scalingExceedsMax = true;
      }
    } else { // Landscape or square
      if (potentialHeight > maxShortSize || potentialWidth > maxLongSize) {
        scalingExceedsMax = true;
      }
    }

    if (!scalingExceedsMax) {
      // Safe to scale up.
      BufferedImage scaledImage = new BufferedImage(potentialWidth, potentialHeight, input.getType());
      Graphics2D g2d = scaledImage.createGraphics();
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g2d.drawImage(input, 0, 0, potentialWidth, potentialHeight, null);
      g2d.dispose();
      return scaledImage;
    } else {
      // Scaling up would exceed max limits.
      // Instead, add a black margin (padding) to meet the min dimension requirement.
      int finalWidth = Math.max(width, minShortSize);
      int finalHeight = Math.max(height, minShortSize);
      BufferedImage padded = new BufferedImage(finalWidth, finalHeight, input.getType());
      Graphics2D g2d = padded.createGraphics();
      // Fill the new image with black.
      g2d.setColor(Color.BLACK);
      g2d.fillRect(0, 0, finalWidth, finalHeight);
      // Center the original image on the canvas.
      int x = (finalWidth - width) / 2;
      int y = (finalHeight - height) / 2;
      g2d.drawImage(input, x, y, null);
      g2d.dispose();
      return padded;
    }
  }
}
