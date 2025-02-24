package com.github.joonasvali.bookreaderai;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class ApplicationUI extends JFrame {
  private Path[] paths;
  private int currentIndex = 0;

  private JLabel imageLabel;
  private JTextArea textArea;
  private JButton prevButton;
  private JButton nextButton;
  private BufferedImage loadedImage;
  private ImagePanel imagePanel;

  private Timer resizeTimer;  // Timer for debounce

  public ApplicationUI(Path[] paths) {
    this.paths = paths;

    try {
      loadedImage = ImageIO.read(paths[currentIndex].toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Initialize the UI components
    initComponents();

    // Set up the frame
    setTitle("Image Viewer");
    setPreferredSize(new Dimension(1600, 1000));
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    pack();
    setLocationRelativeTo(null); // Center the frame
    setVisible(true);

    imagePanel.refreshIcon();
  }

  private void initComponents() {
    // Main panel with BorderLayout
    JPanel mainPanel = new JPanel(new BorderLayout());

    // Center panel for image and text
    JPanel centerPanel = new JPanel(new GridLayout(1, 2));

    // Image label
    imageLabel = new JLabel();
    imageLabel.setHorizontalAlignment(JLabel.CENTER);

    // Text area
    textArea = new JTextArea();
    textArea.setEditable(true);
    JScrollPane textScrollPane = new JScrollPane(textArea);

    // Add image label and text area to center panel
    imagePanel = new ImagePanel(imageLabel);
    centerPanel.add(imagePanel);
    centerPanel.add(textScrollPane);

    // Bottom panel for navigation buttons
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    prevButton = new JButton("Previous");
    nextButton = new JButton("Next");

    // Add action listeners to buttons
    prevButton.addActionListener(e -> showPreviousImage());
    nextButton.addActionListener(e -> showNextImage());

    // Add buttons to bottom panel
    bottomPanel.add(prevButton);
    bottomPanel.add(nextButton);

    // Add panels to main panel
    mainPanel.add(centerPanel, BorderLayout.CENTER);
    mainPanel.add(bottomPanel, BorderLayout.SOUTH);

    // Add main panel to frame
    getContentPane().add(mainPanel);
    // Timer: Call updateDisplay() only once after resizing stops
    resizeTimer = new Timer(100, e -> updateDisplay());
    resizeTimer.setRepeats(false); // Only fire once after delay

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        resizeTimer.restart(); // Reset timer on every resize event
      }
    });

    // Display the first image
    updateDisplay();

  }

  private void showPreviousImage() {
    if (currentIndex > 0) {
      currentIndex--;

      try {
        loadedImage = ImageIO.read(paths[currentIndex].toFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      imagePanel.clearDrawings();
      updateDisplay();
    }
  }

  private void showNextImage() {
    if (currentIndex < paths.length - 1) {
      currentIndex++;

      try {
        loadedImage = ImageIO.read(paths[currentIndex].toFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      imagePanel.clearDrawings();
      updateDisplay();
    }
  }

  private void updateDisplay() {
    if (paths.length == 0 || currentIndex < 0 || currentIndex >= paths.length) {
      return;
    }

    // Update image
    BufferedImage img = loadedImage;

    // Get actual dimensions of the label (after resizing)
    int labelWidth = imageLabel.getWidth();
    int labelHeight = imageLabel.getHeight();

    // If dimensions are invalid, return
    if (labelWidth <= 0 || labelHeight <= 0) {
      return;
    }

    // Get the original image dimensions
    int imgWidth = img.getWidth();
    int imgHeight = img.getHeight();

    // Maintain aspect ratio while fitting within label dimensions
    double scaleX = (double) labelWidth / imgWidth;
    double scaleY = (double) labelHeight / imgHeight;
    double scale = Math.min(scaleX, scaleY);

    int newWidth = (int) (imgWidth * scale);
    int newHeight = (int) (imgHeight * scale);

    // Scale the image while preserving aspect ratio
    Image scaledImg = resizeImage(img, newWidth, newHeight);
    ImageIcon icon = new ImageIcon(scaledImg);

    // Set the scaled icon
    imageLabel.setIcon(icon);

    // Force UI update
    imageLabel.revalidate();
    imageLabel.repaint();

    // Clear text area (or you can load specific text related to the image)
    textArea.setText("");

    // Update buttons' enabled state
    prevButton.setEnabled(currentIndex > 0);
    nextButton.setEnabled(currentIndex < paths.length - 1);

    imagePanel.refreshIcon();
  }

  private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
    BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = resizedImage.createGraphics();

    // Enable high-quality scaling
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
    g2d.dispose(); // Cleanup resources

    return resizedImage;
  }
}
