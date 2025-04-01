package com.github.joonasvali.bookreaderai.imageutil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

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

  // Custom class to hold the result.
  public static class SplitImageResult {
    public BufferedImage[] sections;
    public boolean useBrightOverlay;  // true if overlay is bright (image was determined to be dark)

    public SplitImageResult(BufferedImage[] sections, boolean useBrightOverlay) {
      this.sections = sections;
      this.useBrightOverlay = useBrightOverlay;
    }
  }

  /**
   * Splits the image into verticalPieces. Only slices after the first include an overlapping area
   * of overlapPx pixels at the top, representing an already-processed region.
   * An overlay (bright or dark) is drawn on that overlap so that the overlap stands out.
   *
   * @param image The source image.
   * @param verticalPieces The number of vertical sections to split the image into.
   * @param overlapPx The number of pixels for the overlap in slices (only applied to slices after the first).
   * @param colorTopOverlap If true, the top overlap area will be painted with an overlay.
   * @return A SplitImageResult containing the split image sections and a flag indicating if a bright overlay was used.
   */
  public static SplitImageResult splitImageIntoSections(BufferedImage image, int verticalPieces, int overlapPx, boolean colorTopOverlap) {
    int width = image.getWidth();
    int height = image.getHeight();

    // Automatically decide if we need a bright overlay.
    double avgBrightness = computeAverageBrightness(image);
    boolean useBrightOverlay = (avgBrightness < 128); // use bright overlay (white) when image is dark

    BufferedImage[] results = new BufferedImage[verticalPieces];

    for (int i = 0; i < verticalPieces; i++) {
      int x = 0;
      int y, h;
      if (i == 0) {   // first slice: no top overlap
        y = 0;
        h = height / verticalPieces;
      } else {  // subsequent slices: include the overlapping region at the top
        y = i * height / verticalPieces - overlapPx;
        h = height / verticalPieces + overlapPx;
      }

      // Instead of using getSubimage directly (which returns a shared view),
      // create an independent copy.
      BufferedImage subImage = image.getSubimage(x, y, width, h);
      BufferedImage section = new BufferedImage(width, h, BufferedImage.TYPE_INT_ARGB);
      Graphics2D gSection = section.createGraphics();
      gSection.drawImage(subImage, 0, 0, null);
      gSection.dispose();

      // For slices that contain an overlap region (i>0), paint the top area with overlay.
      if (i > 0 && colorTopOverlap) {
        Graphics2D g = section.createGraphics();

        // Choose overlay color based on image brightness.
        Color overlayColor = useBrightOverlay
            ? new Color(255, 255, 255, 80)   // semi-transparent white for dark images
            : new Color(0, 0, 0, 80);        // semi-transparent black for light images

        g.setColor(overlayColor);
        // Fill the entire overlap region on the top (exactly overlapPx pixels).
        g.fillRect(0, 0, width, overlapPx);

        // Draw a red separator line at the bottom edge of the overlay.
        g.setColor(Color.RED);
        g.drawLine(0, overlapPx - 1, width - 1, overlapPx - 1);

        g.dispose();
      }

      results[i] = section;
    }

    return new SplitImageResult(results, useBrightOverlay && colorTopOverlap);
  }

  /**
   * Computes the average brightness of the given image.
   * For each pixel, brightness is computed as:
   *    brightness = 0.2126 * R + 0.7152 * G + 0.0722 * B
   *
   * @param image The source image.
   * @return The average brightness (0-255 scale).
   */
  private static double computeAverageBrightness(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    long totalPixels = (long) width * height;
    double sumBrightness = 0;

    // Full pass through the image pixels.
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int rgb = image.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8)  & 0xFF;
        int b = rgb & 0xFF;

        double brightness = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        sumBrightness += brightness;
      }
    }
    return sumBrightness / totalPixels;
  }
//
//  // Optionally include a main() method for testing.
//  public static void main(String[] args) throws InterruptedException, InvocationTargetException, IOException {
//    // Example of loading an image, splitting it, and then inspecting the result.
//    // BufferedImage image = ImageIO.read(new File("pathToYourImage"));
//    // SplitImageResult result = splitImageIntoSections(image, 3, 10, true);
//    // System.out.println("Use bright overlay? " + result.useBrightOverlay);
//    // Then save the result sections as needed.
//
//    // For testing, we can also use a simple example image.
//    BufferedImage testImage = ImageIO.read(new File("C:\\Users\\Joonas\\Desktop\\xxx.jpg"));
//
//    SplitImageResult result = splitImageIntoSections(testImage, 5, 50, true);
//    System.out.println("Use bright overlay? " + result.useBrightOverlay);
//
//    // show images in a single JFrame:
//    SwingUtilities.invokeAndWait(() -> {
//      JFrame frame = new JFrame();
//      frame.setLayout(new GridLayout(1, 3));
//      for (BufferedImage section : result.sections) {
//        frame.add(new JLabel(new ImageIcon(section)));
//      }
//      frame.pack();
//      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//      frame.setVisible(true);
//    });
//
//    // For testing, we can also save the result sections as images.
//
//  }

}