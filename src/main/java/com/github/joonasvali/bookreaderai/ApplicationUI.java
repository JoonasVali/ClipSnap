package com.github.joonasvali.bookreaderai;

import com.github.joonasvali.bookreaderai.textutil.LineBreaker;
import com.github.joonasvali.bookreaderai.transcribe.JoinedTranscriber;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class ApplicationUI extends JFrame {
  private Path[] paths;
  private int currentIndex = 0;

  private JLabel imageLabel;
  private JTextArea textArea;
  private JButton prevButton;
  private JButton nextButton;
  private JButton saveButton;
  private BufferedImage loadedImage;
  private ImagePanel imagePanel;
  private JProgressBar bar;
  private final String outputFolder;
  private Path fileTxtPath;
  private final FileHandler fileHandler;
  private String inputFileName;

  private Timer resizeTimer;  // Timer for debounce

  public ApplicationUI(Path[] paths, String outputFolder) {
    this.paths = paths;
    this.outputFolder = outputFolder;

    this.fileHandler = new FileHandler(outputFolder);

    try {
      loadedImage = ImageIO.read(paths[currentIndex].toFile());
      inputFileName = getFileNameWithoutSuffix(paths[currentIndex]);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Initialize the UI components
    initComponents();
    calculateFileOutputPath();
    loadContent();

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
    bar = new JProgressBar();

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
    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    SpinnerModel model = new SpinnerNumberModel(3, 1, 8, 1);
    JSpinner zoomLevel = new JSpinner(model);

    saveButton = new JButton("Save");

    JButton askButton = new JButton("Transcribe");
    topPanel.add(zoomLevel);
    topPanel.add(askButton);
    topPanel.add(bar);
    topPanel.add(saveButton);

    prevButton = new JButton("Previous");
    nextButton = new JButton("Next");

    // Add action listeners to buttons
    prevButton.addActionListener(e -> showPreviousImage());
    nextButton.addActionListener(e -> showNextImage());

    // Add action listener to saveButton
    saveButton.addActionListener(e -> {
      saveContent();
    });

    askButton.addActionListener(e -> {
      bar.setValue(5); // Dummy value to show progress bar is working

      ProgressUpdateUtility progressUpdateUtility = new ProgressUpdateUtility((Integer) zoomLevel.getValue());
      BufferedImage[] images = ImageCutter.cutImage(loadedImage, (Integer) zoomLevel.getValue(), 50);

      Consumer<Float> listener = progress -> {
        SwingUtilities.invokeLater(() -> bar.setValue((int) (progress * 100)));
      };
      progressUpdateUtility.setListener(listener);


      JoinedTranscriber transcriber = new JoinedTranscriber(images, "estonian", "This story is historical from around ww2. ");
      transcriber.setProgressUpdateUtility(progressUpdateUtility);


      try {
        transcriber.transcribeImages(result -> {
          System.out.println("Prompt tokens: " + result.promptTokens());
          System.out.println("Completion tokens: " + result.completionTokens());
          System.out.println("Total tokens: " + result.totalTokens());
          LineBreaker lineBreaker = new LineBreaker();
          textArea.setText(lineBreaker.lineBreakAfterEvery(result.text(), 100));
          progressUpdateUtility.removeListener(listener);
          bar.setValue(0);
        });
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }

//
//      // Get screen size
//      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//      int screenWidth = screenSize.width;
//      int screenHeight = screenSize.height;
//
//      // Max size for each image
//      int maxImageWidth = screenWidth / images.length - 20;
//      int maxImageHeight = screenHeight - 100;
//
//      // Resize images if necessary
//      BufferedImage[] resizedImages = new BufferedImage[images.length];
//      for (int i = 0; i < images.length; i++) {
//        BufferedImage originalImage = images[i];
//        int width = originalImage.getWidth();
//        int height = originalImage.getHeight();
//
//        double widthScale = (double) maxImageWidth / width;
//        double heightScale = (double) maxImageHeight / height;
//        double scale = Math.min(widthScale, heightScale);
//
//        if (scale < 1.0) {
//          int newWidth = (int) (width * scale);
//          int newHeight = (int) (height * scale);
//          Image tmp = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
//          BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
//          Graphics2D g2d = resized.createGraphics();
//          g2d.drawImage(tmp, 0, 0, null);
//          g2d.dispose();
//          resizedImages[i] = resized;
//        } else {
//          resizedImages[i] = originalImage;
//        }
//      }
//
//      // Open JFrame with dynamic number of images
//      JFrame frame = new JFrame();
//      frame.setLayout(new GridLayout(1, images.length));
//
//      for (BufferedImage img : resizedImages) {
//        frame.add(new JLabel(new ImageIcon(img)));
//      }
//
//      frame.pack();
//      frame.setVisible(true);
    });

    // Add buttons to bottom panel
    bottomPanel.add(prevButton);
    bottomPanel.add(nextButton);

    mainPanel.add(topPanel, BorderLayout.NORTH);
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

  private void loadContent() {
    try {
      String content = fileHandler.loadFromFile(inputFileName);
      textArea.setText(content);
    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, "Failed to load file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void saveContent() {
    String content = textArea.getText();
    if (content.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Text area is empty. Nothing to save.", "Warning", JOptionPane.WARNING_MESSAGE);
      return;
    }

    try {
      fileHandler.saveToFile(inputFileName, content);
      JOptionPane.showMessageDialog(this, "File saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Failed to save file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private boolean isUnsavedChanges() {
    try {
      String currentContent = textArea.getText();
      String savedContent = fileHandler.loadFromFile(inputFileName);
      return !currentContent.equals(savedContent);
    } catch (IOException e) {
      return true; // Assume there are unsaved changes if an error occurs
    }
  }

  private void showPreviousImage() {
    if (currentIndex > 0) {
      if (isUnsavedChanges()) {
        int option = JOptionPane.showConfirmDialog(this, "You have unsaved changes. Do you want to save them before moving to the next image?", "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
        if (option == JOptionPane.CANCEL_OPTION) {
          return; // Do nothing if the user cancels
        } else if (option == JOptionPane.YES_OPTION) {
          saveContent(); // Save the content if the user chooses to save
        }
      }

      currentIndex--;
      inputFileName = getFileNameWithoutSuffix(paths[currentIndex]);
      calculateFileOutputPath();
      loadContent();

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
      if (isUnsavedChanges()) {
        int option = JOptionPane.showConfirmDialog(this, "You have unsaved changes. Do you want to save them before moving to the next image?", "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
        if (option == JOptionPane.CANCEL_OPTION) {
          return; // Do nothing if the user cancels
        } else if (option == JOptionPane.YES_OPTION) {
          saveContent(); // Save the content if the user chooses to save
        }
      }

      currentIndex++;
      inputFileName = getFileNameWithoutSuffix(paths[currentIndex]);
      calculateFileOutputPath();
      loadContent();

      try {
        loadedImage = ImageIO.read(paths[currentIndex].toFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      imagePanel.clearDrawings();
      updateDisplay();
    }
  }

  private String getFileNameWithoutSuffix(Path path) {
    String fileName = path.getFileName().toString();
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
  }

  private void calculateFileOutputPath() {
    fileTxtPath = Paths.get(outputFolder, currentIndex + ".txt");
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
