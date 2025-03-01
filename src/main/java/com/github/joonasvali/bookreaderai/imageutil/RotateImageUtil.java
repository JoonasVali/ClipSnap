package com.github.joonasvali.bookreaderai.imageutil;

import java.awt.*;
import java.awt.image.BufferedImage;

public class RotateImageUtil {
  /**
   * Rotates the image 90° clockwise.
   * @param img the image to rotate
   * @return the rotated image
   */
  public static BufferedImage rotate90(BufferedImage img) {
    int w = img.getWidth();
    int h = img.getHeight();
    BufferedImage rotated = new BufferedImage(h, w, img.getType());
    Graphics2D g2d = rotated.createGraphics();
    g2d.translate(h, 0);
    g2d.rotate(Math.toRadians(90));
    g2d.drawImage(img, 0, 0, null);
    g2d.dispose();
    return rotated;
  }

  /**
   * Rotates the image by the given number of 90° clockwise rotations.
   * @param img the image to rotate
   *            (the original image is not modified)
   */
  public static BufferedImage applyRotation(BufferedImage img, int rotationCount) {
    BufferedImage result = img;
    for (int i = 0; i < rotationCount % 4; i++) {
      result = RotateImageUtil.rotate90(result);
    }
    return result;
  }
}
