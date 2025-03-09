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
   * the original image is returned.
   *
   * @param input the original BufferedImage
   * @return a BufferedImage with both dimensions at least {minShortSize}px.
   */
  public BufferedImage resizeImageToMinLimits(BufferedImage input) {
    int width = input.getWidth();
    int height = input.getHeight();

    // If both dimensions are already at least {minShortSize} px, no scaling is needed.
    if (width >= minShortSize && height >= minShortSize) {
      return input;
    }

    // Calculate the scale factors needed for each dimension.
    double scaleFactorWidth = (double) minShortSize / width;
    double scaleFactorHeight = (double) minShortSize / height;

    // Choose the larger scale factor so that both dimensions become at least {minShortSize}.
    double scaleFactor = Math.max(scaleFactorWidth, scaleFactorHeight);

    int newWidth = (int) Math.round(width * scaleFactor);
    int newHeight = (int) Math.round(height * scaleFactor);

    // Create a new BufferedImage with the scaled dimensions.
    BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, input.getType());
    Graphics2D g2d = scaledImage.createGraphics();

    // Use high-quality scaling.
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g2d.drawImage(input, 0, 0, newWidth, newHeight, null);
    g2d.dispose();

    return scaledImage;
  }
}
