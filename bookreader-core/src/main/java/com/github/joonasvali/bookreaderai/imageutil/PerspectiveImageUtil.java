package com.github.joonasvali.bookreaderai.imageutil;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

public class PerspectiveImageUtil {

  public static BufferedImage normalizeImageToRectangle(BufferedImage image, Point[] points) {
    if (points == null || points.length != 4) {
      throw new IllegalArgumentException("Exactly 4 points are required.");
    }
    Point[] pts = reorderPoints(points);

    // Compute destination width: maximum of the top and bottom edge lengths.
    double widthA = pts[0].distance(pts[1]);
    double widthB = pts[3].distance(pts[2]);
    int destWidth = (int) Math.round(Math.max(widthA, widthB));

    // Compute destination height: maximum of the left and right edge lengths.
    double heightA = pts[0].distance(pts[3]);
    double heightB = pts[1].distance(pts[2]);
    int destHeight = (int) Math.round(Math.max(heightA, heightB));

    // Define destination rectangle corners (top-left, top-right, bottom-right, bottom-left)
    Point2D.Double[] dst = new Point2D.Double[4];
    dst[0] = new Point2D.Double(0, 0);
    dst[1] = new Point2D.Double(destWidth - 1, 0);
    dst[2] = new Point2D.Double(destWidth - 1, destHeight - 1);
    dst[3] = new Point2D.Double(0, destHeight - 1);

    // Compute the perspective transform (homography) that maps pts -> dst
    double[][] H = getPerspectiveTransform(pts, dst);

    // To easily sample from the source image, compute the inverse transform.
    double[][] Hinv = invert3x3Matrix(H);

    BufferedImage destImage = new BufferedImage(destWidth, destHeight, image.getType());

    // Map every pixel (x,y) in the destination image back to the source image.
    for (int y = 0; y < destHeight; y++) {
      for (int x = 0; x < destWidth; x++) {
        double[] srcPt = applyTransform(Hinv, x, y);
        int srcX = (int) Math.round(srcPt[0]);
        int srcY = (int) Math.round(srcPt[1]);
        // If the computed source pixel is within bounds, copy its color; otherwise, paint black.
        if (srcX >= 0 && srcX < image.getWidth() && srcY >= 0 && srcY < image.getHeight()) {
          destImage.setRGB(x, y, image.getRGB(srcX, srcY));
        } else {
          destImage.setRGB(x, y, 0); // black pixel
        }
      }
    }

    return destImage;
  }

  // Computes the 3x3 perspective transform matrix (homography) that maps
  // the source points (pts) to the destination points (dst).
  private static double[][] getPerspectiveTransform(Point[] src, Point2D.Double[] dst) {
    // We will solve for the 8 unknowns (the 9th element is fixed to 1).
    // For each correspondence (x, y) -> (u, v), the equations are:
    //   a*x + b*y + c - u*(g*x + h*y + 1) = 0
    //   d*x + e*y + f - v*(g*x + h*y + 1) = 0
    // We arrange these into an 8x8 system: A * h = B
    double[][] A = new double[8][8];
    double[] B = new double[8];

    for (int i = 0; i < 4; i++) {
      double x = src[i].getX();
      double y = src[i].getY();
      double u = dst[i].getX();
      double v = dst[i].getY();

      // Equation for u
      A[2 * i][0] = x;
      A[2 * i][1] = y;
      A[2 * i][2] = 1;
      A[2 * i][3] = 0;
      A[2 * i][4] = 0;
      A[2 * i][5] = 0;
      A[2 * i][6] = -x * u;
      A[2 * i][7] = -y * u;
      B[2 * i] = u;

      // Equation for v
      A[2 * i + 1][0] = 0;
      A[2 * i + 1][1] = 0;
      A[2 * i + 1][2] = 0;
      A[2 * i + 1][3] = x;
      A[2 * i + 1][4] = y;
      A[2 * i + 1][5] = 1;
      A[2 * i + 1][6] = -x * v;
      A[2 * i + 1][7] = -y * v;
      B[2 * i + 1] = v;
    }

    // Solve the linear system A * h = B for h (8 unknowns)
    double[] h = solveLinearSystem(A, B);

    // Construct the 3x3 matrix H (the last element is set to 1)
    double[][] H = new double[3][3];
    H[0][0] = h[0];
    H[0][1] = h[1];
    H[0][2] = h[2];
    H[1][0] = h[3];
    H[1][1] = h[4];
    H[1][2] = h[5];
    H[2][0] = h[6];
    H[2][1] = h[7];
    H[2][2] = 1;
    return H;
  }

  // Solves an 8x8 linear system using Gaussian elimination.
  private static double[] solveLinearSystem(double[][] A, double[] B) {
    int n = B.length;
    double[][] M = new double[n][n + 1];

    // Build augmented matrix.
    for (int i = 0; i < n; i++) {
      System.arraycopy(A[i], 0, M[i], 0, n);
      M[i][n] = B[i];
    }

    // Forward elimination.
    for (int i = 0; i < n; i++) {
      // Find pivot row.
      int max = i;
      for (int j = i + 1; j < n; j++) {
        if (Math.abs(M[j][i]) > Math.abs(M[max][i])) {
          max = j;
        }
      }
      // Swap current row with pivot row.
      double[] temp = M[i];
      M[i] = M[max];
      M[max] = temp;

      if (Math.abs(M[i][i]) < 1e-10) {
        throw new RuntimeException("Matrix is singular or nearly singular");
      }

      // Eliminate entries below the pivot.
      for (int j = i + 1; j < n; j++) {
        double factor = M[j][i] / M[i][i];
        for (int k = i; k <= n; k++) {
          M[j][k] -= factor * M[i][k];
        }
      }
    }

    // Back substitution.
    double[] x = new double[n];
    for (int i = n - 1; i >= 0; i--) {
      double sum = M[i][n];
      for (int j = i + 1; j < n; j++) {
        sum -= M[i][j] * x[j];
      }
      x[i] = sum / M[i][i];
    }
    return x;
  }

  // Inverts a 3x3 matrix.
  private static double[][] invert3x3Matrix(double[][] m) {
    double[][] inv = new double[3][3];
    double det = m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1])
        - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
        + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
    if (Math.abs(det) < 1e-10) {
      throw new RuntimeException("Matrix is singular and cannot be inverted");
    }
    inv[0][0] = (m[1][1] * m[2][2] - m[1][2] * m[2][1]) / det;
    inv[0][1] = (m[0][2] * m[2][1] - m[0][1] * m[2][2]) / det;
    inv[0][2] = (m[0][1] * m[1][2] - m[0][2] * m[1][1]) / det;
    inv[1][0] = (m[1][2] * m[2][0] - m[1][0] * m[2][2]) / det;
    inv[1][1] = (m[0][0] * m[2][2] - m[0][2] * m[2][0]) / det;
    inv[1][2] = (m[0][2] * m[1][0] - m[0][0] * m[1][2]) / det;
    inv[2][0] = (m[1][0] * m[2][1] - m[1][1] * m[2][0]) / det;
    inv[2][1] = (m[0][1] * m[2][0] - m[0][0] * m[2][1]) / det;
    inv[2][2] = (m[0][0] * m[1][1] - m[0][1] * m[1][0]) / det;
    return inv;
  }

  // Applies a 3x3 transformation matrix H to a point (x, y)
  // and returns the transformed coordinates.
  private static double[] applyTransform(double[][] H, double x, double y) {
    double denominator = H[2][0] * x + H[2][1] * y + H[2][2];
    double newX = (H[0][0] * x + H[0][1] * y + H[0][2]) / denominator;
    double newY = (H[1][0] * x + H[1][1] * y + H[1][2]) / denominator;
    return new double[]{newX, newY};
  }

  /**
   * Reorders an array of four points into the following order:
   * 1. Top-left
   * 2. Top-right
   * 3. Bottom-right
   * 4. Bottom-left
   *
   * The algorithm assumes a coordinate system where x increases to the right and y increases downward.
   * It calculates the sum (x+y) for each point:
   * - The point with the smallest sum is the top-left.
   * - The point with the largest sum is the bottom-right.
   * Then, it calculates the difference (y-x) for each point:
   * - The point with the smallest difference is the top-right.
   * - The point with the largest difference is the bottom-left.
   *
   * @param points an array of 4 points in arbitrary order.
   * @return a new array of points in the order: top-left, top-right, bottom-right, bottom-left.
   */
  public static Point[] reorderPoints(Point[] points) {
    if (points == null || points.length != 4) {
      throw new IllegalArgumentException("Exactly 4 points are required.");
    }

    Point topLeft = points[0];
    Point topRight = points[0];
    Point bottomRight = points[0];
    Point bottomLeft = points[0];

    double minSum = Double.POSITIVE_INFINITY;
    double maxSum = Double.NEGATIVE_INFINITY;
    double minDiff = Double.POSITIVE_INFINITY;
    double maxDiff = Double.NEGATIVE_INFINITY;

    for (Point p : points) {
      double sum = p.x + p.y;
      double diff = p.y - p.x;

      if (sum < minSum) {
        minSum = sum;
        topLeft = p;
      }
      if (sum > maxSum) {
        maxSum = sum;
        bottomRight = p;
      }
      if (diff < minDiff) {
        minDiff = diff;
        topRight = p;
      }
      if (diff > maxDiff) {
        maxDiff = diff;
        bottomLeft = p;
      }
    }

    return new Point[]{ topLeft, topRight, bottomRight, bottomLeft };
  }

  public static boolean arePointsAtTheCornersOfImage(BufferedImage image, Point[] points) {
    int width = image.getWidth();
    int height = image.getHeight();

    // There must be exactly 4 points
    if (points == null || points.length != 4) {
      return false;
    }

    // Create a set of the expected corner points
    Set<Point> expectedCorners = new HashSet<>();
    expectedCorners.add(new Point(0, 0));
    expectedCorners.add(new Point(width, 0));
    expectedCorners.add(new Point(0, height));
    expectedCorners.add(new Point(width, height));

    // Check each provided point
    for (Point point : points) {
      // Check that the point is at one of the corners:
      // x must be 0 or width, AND y must be 0 or height.
      if (!((point.x == 0 || point.x == width) && (point.y == 0 || point.y == height))) {
        return false;
      }
      // Remove the found corner from the set. If a duplicate is provided,
      // the expectedCorners set won't be emptied.
      expectedCorners.remove(new Point(point.x, point.y));
    }

    // If every corner was provided exactly once, the set will be empty.
    return expectedCorners.isEmpty();
  }
}