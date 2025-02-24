package com.github.joonasvali.bookreaderai;

import java.awt.*;
import java.awt.geom.Line2D;

public class Line {
  Point start, end;

  public Line(Point start, Point end) {
    this.start = start;
    this.end = end;
  }

  public boolean isNearStart(Point p) {
    return start.distance(p) < 10;
  }

  public boolean isNearEnd(Point p) {
    return end.distance(p) < 10;
  }

  public boolean isNearLine(Point p) {
    return Line2D.ptSegDist(start.x, start.y, end.x, end.y, p.x, p.y) < 5.0;
  }
}
