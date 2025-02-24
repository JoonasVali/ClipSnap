package com.github.joonasvali.bookreaderai;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageCutter {
  public static BufferedImage[] cutImage(BufferedImage image, Line line) {
    int width = image.getWidth();
    int height = image.getHeight();
    BufferedImage image1 = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    BufferedImage image2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    Point start = line.start;
    Point end = line.end;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int position = (x - start.x) * (end.y - start.y) - (y - start.y) * (end.x - start.x);

        int rgb = image.getRGB(x, y);

        if (position > 0) {
          image1.setRGB(x, y, rgb);
          image2.setRGB(x, y, Color.BLACK.getRGB());
        } else if (position < 0) {
          image1.setRGB(x, y, Color.BLACK.getRGB());
          image2.setRGB(x, y, rgb);
        } else {
          image1.setRGB(x, y, rgb);
          image2.setRGB(x, y, rgb);
        }
      }
    }

    return new BufferedImage[]{image1, image2};
  }
}