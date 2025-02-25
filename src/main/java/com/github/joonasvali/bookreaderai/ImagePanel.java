package com.github.joonasvali.bookreaderai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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
    private Point startPoint; // For drawing new lines
    private Line selectedLine; // Line selected for moving endpoints
    private boolean draggingStartPoint; // Indicates which endpoint is dragged
    private Point currentPoint; // Current point for temporary line
    private Point mousePoint; // Current mouse position

    public DrawingPanel() {
      setOpaque(false);

      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          Point p = e.getPoint();

          if (e.getButton() == MouseEvent.BUTTON3) { // Right-click to delete line
            Line lineToRemove = null;
            for (Line line : lines) {
              if (line.isNearLine(p)) {
                lineToRemove = line;
                break;
              }
            }
            if (lineToRemove != null) {
              lines.remove(lineToRemove);
              repaint();
            }
            return;
          }

          selectedLine = null;

          // Check if the click is near any line endpoint
          for (Line line : lines) {
            if (line.isNearStart(p)) {
              selectedLine = line;
              draggingStartPoint = true;
              break;
            } else if (line.isNearEnd(p)) {
              selectedLine = line;
              draggingStartPoint = false;
              break;
            }
          }

          if (selectedLine == null) {
            // Start drawing a new line
            startPoint = p;
          }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          if (startPoint != null && currentPoint != null) {
            // Add new line to the list
            lines.add(new Line(startPoint, currentPoint));
            startPoint = null;
            currentPoint = null;
            repaint();
          }
          if (selectedLine != null) {
            selectedLine = null;
            repaint();
          }
        }
      });

      addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
          Point p = e.getPoint();
          if (selectedLine != null) {
            // Move the selected endpoint
            if (draggingStartPoint) {
              selectedLine.start = p;
            } else {
              selectedLine.end = p;
            }
            repaint();
          } else if (startPoint != null) {
            // Update current point for temporary line
            currentPoint = p;
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

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      g.setColor(Color.RED);
      Graphics2D g2 = (Graphics2D) g;
      g2.setStroke(new BasicStroke(2));

      // Draw all lines
      for (Line line : lines) {
        g2.drawLine(line.start.x, line.start.y, line.end.x, line.end.y);

        // Draw start point
        if (mousePoint != null && line.isNearStart(mousePoint)) {
          g2.setColor(Color.CYAN);
          g2.fillOval(line.start.x - 3, line.start.y - 3, 6, 6);
          g2.setColor(Color.RED);
        } else {
          g2.fillOval(line.start.x - 3, line.start.y - 3, 6, 6);
        }

        // Draw end point
        if (mousePoint != null && line.isNearEnd(mousePoint)) {
          g2.setColor(Color.CYAN);
          g2.fillOval(line.end.x - 3, line.end.y - 3, 6, 6);
          g2.setColor(Color.RED);
        } else {
          g2.fillOval(line.end.x - 3, line.end.y - 3, 6, 6);
        }
      }

      // Draw temporary line while drawing
      if (startPoint != null && currentPoint != null) {
        g2.drawLine(startPoint.x, startPoint.y, currentPoint.x, currentPoint.y);

        // Draw start point of temporary line
        if (mousePoint != null && startPoint.distance(mousePoint) < 10) {
          g2.setColor(Color.CYAN);
          g2.fillOval(startPoint.x - 3, startPoint.y - 3, 6, 6);
          g2.setColor(Color.RED);
        } else {
          g2.fillOval(startPoint.x - 3, startPoint.y - 3, 6, 6);
        }

        // Draw current point of temporary line
        if (mousePoint != null && currentPoint.distance(mousePoint) < 10) {
          g2.setColor(Color.CYAN);
          g2.fillOval(currentPoint.x - 3, currentPoint.y - 3, 6, 6);
          g2.setColor(Color.RED);
        } else {
          g2.fillOval(currentPoint.x - 3, currentPoint.y - 3, 6, 6);
        }
      }
    }

    public void clear() {
      lines.clear();
      repaint();
    }

    public ArrayList<Line> getLines() {
      return lines;
    }
  }


  public ArrayList<Line> getLines() {
    return drawingPanel.getLines();
  }
}
