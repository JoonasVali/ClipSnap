package com.github.joonasvali.bookreaderai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class ImagePanel extends JPanel {
  private Image image;
  private final ArrayList<Line> lines = new ArrayList<>();
  private Point startPoint;

  public ImagePanel(ImageIcon imageIcon) {
    this.image = imageIcon.getImage();

    // Mouse listeners to draw lines
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

    // Draw the image
    g.drawImage(image, 0, 0, getWidth(), getHeight(), this);

    // Draw the lines
    g.setColor(Color.RED);
    for (Line line : lines) {
      g.drawLine(line.start.x, line.start.y, line.end.x, line.end.y);
    }
  }

  // Line class to store start and end points
  private static class Line {
    Point start, end;
    public Line(Point start, Point end) {
      this.start = start;
      this.end = end;
    }
  }

}
