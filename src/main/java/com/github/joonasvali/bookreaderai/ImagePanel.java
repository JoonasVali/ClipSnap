package com.github.joonasvali.bookreaderai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class ImagePanel extends JLayeredPane {
  private final JLabel imageLabel;
  private final DrawingPanel drawingPanel;

  public ImagePanel(JLabel imageLabel) {
    this.imageLabel = imageLabel;
    this.drawingPanel = new DrawingPanel();

    // Use null layout for absolute positioning.
    setLayout(null);

    // Set a default size so that drawing is possible even if no icon is set yet.
    int defaultWidth = 100;
    int defaultHeight = 100;
    if (imageLabel.getIcon() != null) {
      defaultWidth = imageLabel.getIcon().getIconWidth();
      defaultHeight = imageLabel.getIcon().getIconHeight();
    }
    imageLabel.setBounds(0, 0, defaultWidth, defaultHeight);
    drawingPanel.setBounds(0, 0, defaultWidth, defaultHeight);
    setPreferredSize(new Dimension(defaultWidth, defaultHeight));

    // Add the image label to the background layer.
    add(imageLabel, Integer.valueOf(0));

    // Add the transparent drawing panel to the top layer.
    add(drawingPanel, Integer.valueOf(1));
  }

  /**
   * This method should be called when the icon becomes available (or changes).
   * It updates the bounds of both the imageLabel and drawingPanel.
   */
  public void refreshIcon() {
    if (imageLabel.getIcon() == null) {
      return; // No image yet, do nothing
    }

    int panelWidth = getWidth();
    int panelHeight = getHeight();

    if (panelWidth == 0 || panelHeight == 0) {
      return; // Prevent resizing issues before layout is set
    }

    // Set bounds to fill ImagePanel properly
    imageLabel.setBounds(0, 0, panelWidth, panelHeight);
    drawingPanel.setBounds(0, 0, panelWidth, panelHeight);
    setPreferredSize(new Dimension(panelWidth, panelHeight));

    // Initialize or update the crop rectangle corners to match the new bounds
    drawingPanel.initializeCorners(panelWidth, panelHeight);

    revalidate();
    repaint();
  }

  /**
   * Optionally clear the crop rectangle (reset to full image).
   */
  public void resetCropRectangle() {
    drawingPanel.resetCorners();
  }

  /**
   * Returns the current crop rectangle corners.
   * @return Array of four Points representing the crop rectangle corners.
   */
  public Point[] getCropCorners() {
    return drawingPanel.getCorners();
  }

  /**
   * The transparent panel on top of the image that handles drawing the crop rectangle.
   */
  private static class DrawingPanel extends JPanel {
    private Point[] corners = null; // Array of 4 points for the rectangle corners
    private int selectedCornerIndex = -1; // Index of the corner being dragged
    private Point mousePoint; // Current mouse position
    private static final int MARKER_RADIUS = 6; // For a marker diameter of 12
    private static final int DRAG_THRESHOLD = 10; // Threshold to select a corner

    public DrawingPanel() {
      setOpaque(false);

      // Initialize corners once the component has a size.
      addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
          if (corners == null) {
            int w = getWidth();
            int h = getHeight();
            initializeCorners(w, h);
          }
        }
      });

      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          Point p = e.getPoint();
          // Check if the click is near any of the corners
          if (corners != null) {
            for (int i = 0; i < corners.length; i++) {
              if (p.distance(corners[i]) < DRAG_THRESHOLD) {
                selectedCornerIndex = i;
                break;
              }
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
        public void mouseDragged(MouseEvent e) {
          if (selectedCornerIndex != -1 && corners != null) {
            // Clamp the dragged point within the panel bounds
            int newX = e.getX();
            int newY = e.getY();
            int w = getWidth();
            int h = getHeight();
            if (newX < 0) newX = 0;
            if (newY < 0) newY = 0;
            if (newX > w) newX = w;
            if (newY > h) newY = h;
            corners[selectedCornerIndex] = new Point(newX, newY);
            repaint();
          }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
          mousePoint = e.getPoint();
          repaint();
        }
      });
    }

    /**
     * Initializes the corners of the crop rectangle to match the given width and height.
     */
    public void initializeCorners(int width, int height) {
      if (corners == null) {
        corners = new Point[4];
      }
      corners[0] = new Point(0, 0);         // top-left
      corners[1] = new Point(width, 0);       // top-right
      corners[2] = new Point(width, height);  // bottom-right
      corners[3] = new Point(0, height);      // bottom-left
      repaint();
    }

    /**
     * Resets the crop rectangle to cover the full area.
     */
    public void resetCorners() {
      if (getWidth() > 0 && getHeight() > 0) {
        initializeCorners(getWidth(), getHeight());
      }
    }

    /**
     * Returns the current corners of the crop rectangle.
     */
    public Point[] getCorners() {
      return corners;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (corners == null) return;
      Graphics2D g2 = (Graphics2D) g;
      g2.setColor(Color.RED);
      g2.setStroke(new BasicStroke(2));

      // Draw lines connecting the corners (with wrap-around)
      for (int i = 0; i < corners.length; i++) {
        Point p1 = corners[i];
        Point p2 = corners[(i + 1) % corners.length];
        g2.drawLine(p1.x, p1.y, p2.x, p2.y);
      }

      // Draw larger markers for the corners
      for (Point corner : corners) {
        if (mousePoint != null && mousePoint.distance(corner) < DRAG_THRESHOLD) {
          g2.setColor(Color.CYAN);
        } else {
          g2.setColor(Color.RED);
        }
        // Draw a circle centered at the corner with a diameter of 12
        g2.fillOval(corner.x - MARKER_RADIUS, corner.y - MARKER_RADIUS, MARKER_RADIUS * 2, MARKER_RADIUS * 2);
      }
    }
  }

  /**
   * Optionally, if needed for external usage.
   */
  public Point[] getCorners() {
    return drawingPanel.getCorners();
  }
}
