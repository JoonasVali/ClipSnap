package com.github.joonasvali.bookreaderai;

import java.awt.image.BufferedImage;

public class ImageCutter {
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