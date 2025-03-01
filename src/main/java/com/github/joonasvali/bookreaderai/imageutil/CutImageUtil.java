package com.github.joonasvali.bookreaderai.imageutil;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CutImageUtil {

  /**
   * Cuts out the polygon defined by the given 4 points from the image.
   * The pixels inside the polygon remain from the original image,
   * while pixels outside the polygon are set to black.
   * The resulting image is then cropped to the polygon's bounding box.
   *
   * @param image  the source BufferedImage
   * @param points an array of 4 Points defining a closed polygon in order
   * @return a new BufferedImage containing only the polygon area with minimal dimensions
   * @throws IllegalArgumentException if points is null or does not contain exactly 4 points
   */
  public static BufferedImage cutImage(BufferedImage image, Point[] points) {
    if (points == null || points.length != 4) {
      throw new IllegalArgumentException("Exactly four points are required.");
    }

    // Create the polygon from the provided points.
    Polygon polygon = new Polygon();
    for (Point p : points) {
      polygon.addPoint(p.x, p.y);
    }

    // Create a new image with the same dimensions and type as the original.
    BufferedImage maskedImage = new BufferedImage(
        image.getWidth(), image.getHeight(), image.getType());

    // Use Graphics2D to first fill the entire image with black.
    Graphics2D g2d = maskedImage.createGraphics();
    g2d.setColor(Color.BLACK);
    g2d.fillRect(0, 0, image.getWidth(), image.getHeight());

    // Set the clipping region to the polygon and draw the original image.
    g2d.setClip(polygon);
    g2d.drawImage(image, 0, 0, null);
    g2d.dispose();

    // Determine the minimal bounding box of the polygon.
    Rectangle bounds = polygon.getBounds();

    // Crop the image to this bounding rectangle.
    BufferedImage croppedImage = maskedImage.getSubimage(
        bounds.x, bounds.y, bounds.width, bounds.height);

    // Copy the cropped image to a new BufferedImage to ensure that its data is independent.
    BufferedImage result = new BufferedImage(
        croppedImage.getWidth(), croppedImage.getHeight(), image.getType());
    Graphics2D g2dResult = result.createGraphics();
    g2dResult.drawImage(croppedImage, 0, 0, null);
    g2dResult.dispose();

    return result;
  }

  public static BufferedImage[] cutImage(BufferedImage image, int verticalPieces, int overlapPx) {
    int width = image.getWidth();
    int height = image.getHeight();

    BufferedImage[] results = new BufferedImage[verticalPieces];
    for (int i = 0; i < verticalPieces; i++) {
      int x = 0;
      int y = i * height / verticalPieces;
      int h = height / verticalPieces;

      if (i > 0) {
        y -= overlapPx;
        h += overlapPx;
      }
      if (i < verticalPieces - 1) {
        h += overlapPx;
      }

      results[i] = image.getSubimage(x, y, width, h);
    }

    return results;
  }
}