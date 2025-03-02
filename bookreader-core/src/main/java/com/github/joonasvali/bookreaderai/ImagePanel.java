package com.github.joonasvali.bookreaderai;

import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

public class ImagePanel extends JLayeredPane {
  private final Logger logger = org.slf4j.LoggerFactory.getLogger(ImagePanel.class);
  private final JLabel imageLabel;
  private final DrawingPanel drawingPanel;
  // Actual full size of the source image in pixels
  private int imageOriginalWidth;
  private int imageOriginalHeight;

  public ImagePanel(JLabel imageLabel) {
    this.imageLabel = imageLabel;
    this.drawingPanel = new DrawingPanel();

    setLayout(null);

    // Default size if icon is null or has no dimension yet
    int defaultWidth = 100;
    int defaultHeight = 100;
    if (imageLabel.getIcon() != null) {
      defaultWidth = imageLabel.getIcon().getIconWidth();
      defaultHeight = imageLabel.getIcon().getIconHeight();
      // By default, also store them here if you like, but typically
      // you'll call refreshIcon(...) with the real sizes from elsewhere.
      imageOriginalWidth = defaultWidth;
      imageOriginalHeight = defaultHeight;
    }

    imageLabel.setBounds(0, 0, defaultWidth, defaultHeight);
    drawingPanel.setBounds(0, 0, defaultWidth, defaultHeight);
    setPreferredSize(new Dimension(defaultWidth, defaultHeight));

    // Add image behind
    add(imageLabel, Integer.valueOf(0));
    // Add the transparent overlay
    add(drawingPanel, Integer.valueOf(1));
  }

  @Override
  public void doLayout() {
    super.doLayout();
    // Whenever layout changes, ensure label & panel fill this container’s size
    int w = getWidth();
    int h = getHeight();
    imageLabel.setBounds(0, 0, w, h);
    drawingPanel.setBounds(0, 0, w, h);
  }

  /**
   * Call this after setting or changing the icon, so the crop rectangle
   * is reinitialized to the displayed image area.
   * @param imageOriginalWidth  The true full width of the source image in pixels.
   * @param imageOriginalHeight The true full height of the source image in pixels.
   */
  public void refreshIcon(int imageOriginalWidth, int imageOriginalHeight) {
    if (imageLabel.getIcon() == null) {
      return;
    }
    // Store the real image dimensions
    this.imageOriginalWidth = imageOriginalWidth;
    this.imageOriginalHeight = imageOriginalHeight;

    // Let the DrawingPanel compute the displayed size & corners
    drawingPanel.initializeCorners();
  }

  /** Reset the crop rectangle so it covers the entire displayed image area. */
  public void resetCropRectangle() {
    drawingPanel.initializeCorners();
  }

  /** Returns the corners in the overlay's (panel) coordinate system. */
  public Point[] getDisplayedCorners() {
    return drawingPanel.getCorners();
  }

  /**
   * Returns the corners in the original (unscaled) image coordinates.
   * This takes into account letterboxing/pillarboxing and the scale factor.
   */
  public Point[] getOriginalCropCoordinates() {
    return drawingPanel.getOriginalCropCoordinates();
  }

  /**
   * Transparent panel for drawing/manipulating a 4-corner crop rectangle.
   */
  private class DrawingPanel extends JPanel {
    private Point[] corners; // The four corners in *panel* coordinates
    private int selectedCornerIndex = -1;
    private Point mousePoint;
    private static final int MARKER_RADIUS = 6;
    private static final int DRAG_THRESHOLD = 10;

    // Variables describing how the image is displayed
    private int displayedImageX, displayedImageY;
    private int displayedImageWidth, displayedImageHeight;
    private double scale; // scale from original image to displayed

    public DrawingPanel() {
      setOpaque(false);

      // Mouse interactions
      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          if (corners == null) return;
          Point p = e.getPoint();
          for (int i = 0; i < corners.length; i++) {
            if (p.distance(corners[i]) < DRAG_THRESHOLD) {
              selectedCornerIndex = i;
              break;
            }
          }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          selectedCornerIndex = -1;
        }
      });

      addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
          mousePoint = e.getPoint();
          repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
          if (corners == null || selectedCornerIndex < 0) return;

          // Clamp corner inside the displayed image region
          int x = Math.max(displayedImageX,
              Math.min(displayedImageX + displayedImageWidth, e.getX()));
          int y = Math.max(displayedImageY,
              Math.min(displayedImageY + displayedImageHeight, e.getY()));

          corners[selectedCornerIndex] = new Point(x, y);
          repaint();
        }
      });
    }

    /**
     * Calculate how the image is displayed (letterbox/pillarbox with aspect ratio),
     * then set the crop corners to the entire displayed image region.
     */
    public void initializeCorners() {
      // If we haven't set the real original width/height, or no icon, bail out
      if (imageLabel.getIcon() == null || imageOriginalWidth <= 0 || imageOriginalHeight <= 0) {
        corners = null;
        return;
      }

      int panelW = getWidth();
      int panelH = getHeight();
      if (panelW <= 0 || panelH <= 0) {
        corners = null;
        return;
      }

      // Compute scale to preserve aspect ratio based on the *real* image size
      double scaleX = (double) panelW / imageOriginalWidth;
      double scaleY = (double) panelH / imageOriginalHeight;
      scale = Math.min(scaleX, scaleY);

      // Actual displayed size of the image
      displayedImageWidth  = (int) Math.round(imageOriginalWidth * scale);
      displayedImageHeight = (int) Math.round(imageOriginalHeight * scale);

      // Center the image in the panel
      displayedImageX = (panelW - displayedImageWidth) / 2;
      displayedImageY = (panelH - displayedImageHeight) / 2;

      // Now set corners to the entire displayed region
      corners = new Point[4];
      corners[0] = new Point(displayedImageX, displayedImageY);
      corners[1] = new Point(displayedImageX + displayedImageWidth, displayedImageY);
      corners[2] = new Point(displayedImageX + displayedImageWidth, displayedImageY + displayedImageHeight);
      corners[3] = new Point(displayedImageX, displayedImageY + displayedImageHeight);
      repaint();
    }

    public Point[] getCorners() {
      return corners;
    }

    /**
     * Convert the four corners into the original (unscaled) image coordinates.
     * Subtract offset (displayedImageX, displayedImageY), then divide by 'scale'.
     */
    public Point[] getOriginalCropCoordinates() {
      if (corners == null) return null;

      Point[] result = new Point[4];
      for (int i = 0; i < 4; i++) {
        // Subtract offset, then divide by scale
        int originalX = Math.max(0, Math.min((int) Math.round((corners[i].x - displayedImageX) / scale), imageOriginalWidth - 1));
        int originalY = Math.max(0, Math.min((int) Math.round((corners[i].y - displayedImageY) / scale), imageOriginalHeight - 1));
        result[i] = new Point(originalX, originalY);
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Got original crop coordinates: {}", Arrays.deepToString(result));
      }
      return result;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (corners == null) return;

      Graphics2D g2 = (Graphics2D) g;
      g2.setStroke(new BasicStroke(2));

      // Draw lines between corners
      g2.setColor(Color.RED);
      for (int i = 0; i < corners.length; i++) {
        Point p1 = corners[i];
        Point p2 = corners[(i + 1) % corners.length];
        g2.drawLine(p1.x, p1.y, p2.x, p2.y);
      }

      // Draw corner markers
      for (Point corner : corners) {
        // Highlight if mouse is close
        if (mousePoint != null && mousePoint.distance(corner) < DRAG_THRESHOLD) {
          g2.setColor(Color.CYAN);
        } else {
          g2.setColor(Color.RED);
        }
        g2.fillOval(corner.x - MARKER_RADIUS, corner.y - MARKER_RADIUS,
            MARKER_RADIUS * 2, MARKER_RADIUS * 2);
      }
    }
  }
}
