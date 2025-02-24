package com.github.joonasvali.bookreaderai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

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

    int panelWidth = getWidth();   // Get the width of ImagePanel
    int panelHeight = getHeight(); // Get the height of ImagePanel

    if (panelWidth == 0 || panelHeight == 0) {
      return; // Prevent resizing issues before layout is set
    }

    // Set bounds to fill ImagePanel properly
    imageLabel.setBounds(0, 0, panelWidth, panelHeight);
    drawingPanel.setBounds(0, 0, panelWidth, panelHeight);
    setPreferredSize(new Dimension(panelWidth, panelHeight));

    revalidate();  // Update layout
    repaint();     // Redraw the panel
  }

  /**
   * Optional: Clears any drawn lines.
   */
  public void clearDrawings() {
    drawingPanel.clear();
  }

  /**
   * The transparent panel on top of the image that handles drawing.
   */
  private static class DrawingPanel extends JPanel {
    private final ArrayList<Line> lines = new ArrayList<>();
    private Point startPoint;

    public DrawingPanel() {
      setOpaque(false); // Transparent so the image shows through.

      // Mouse listener to record the start point and add lines on release.
      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          startPoint = e.getPoint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          if (startPoint != null) {
            lines.add(new Line(startPoint, e.getPoint()));
            startPoint = null;
            repaint();
          }
        }
      });

      // Mouse motion listener to add continuous lines while dragging.
      addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
          if (startPoint != null) {
            lines.add(new Line(startPoint, e.getPoint()));
            startPoint = e.getPoint();
            repaint();
          }
        }
      });
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      g.setColor(Color.RED);
      Graphics2D g2 = (Graphics2D) g;
      g2.setStroke(new BasicStroke(2));
      for (Line line : lines) {
        g2.drawLine(line.start.x, line.start.y, line.end.x, line.end.y);
      }
    }

    public void clear() {
      lines.clear();
      repaint();
    }
  }

  /**
   * Helper class to store line coordinates.
   */
  private static class Line {
    final Point start, end;
    public Line(Point start, Point end) {
      this.start = start;
      this.end = end;
    }
  }
}
