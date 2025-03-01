package com.github.joonasvali.bookreaderai;

import com.github.joonasvali.bookreaderai.imageutil.CutImageUtil;
import com.github.joonasvali.bookreaderai.imageutil.RotateImageUtil;
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
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class ImageContentPanel extends JPanel {
  private static final String PREF_KEY_LAST_IMAGE_INDEX_BASE = "lastImageIndex"; // Preference key
  public static final String PREF_KEY_ROTATION_BASE = "rotation"; // Preference key

  private Path[] paths;
  private int currentIndex = 0;

  private JLabel imageLabel;
  private JTextArea textArea;
  private JButton prevButton;
  private JButton nextButton;
  private JButton saveButton;
  private JButton settingsButton;
  private BufferedImage loadedImage;
  private BufferedImage originalImage; // Store the unrotated image
  private ImagePanel imagePanel;
  private JProgressBar bar;
  private final Path outputFolder;
  private Path fileTxtPath;
  private final FileHandler fileHandler;
  private String inputFileName;
  private Runnable switchToSettingsAction;
  private final TranscriptionHints hints;

  private Timer resizeTimer;  // For debouncing resize events

  public ImageContentPanel(TranscriptionHints hints, Path[] paths, Path outputFolder, Runnable switchToSettingsAction) {
    this.hints = hints;
    this.paths = paths;

    this.outputFolder = outputFolder;
    // Load last image index from preferences
    Preferences prefs = Preferences.userNodeForPackage(ImageContentPanel.class);
    currentIndex = prefs.getInt(getPrefKeyLastImageIndex(), 0);
    // Validate index: if stored index is out of bounds, revert to 0.
    if (currentIndex < 0 || currentIndex >= paths.length) {
      currentIndex = 0;
    }

    this.switchToSettingsAction = switchToSettingsAction;
    this.fileHandler = new FileHandler(outputFolder);

    // Use the helper method to load the image (which also applies rotation)
    inputFileName = getFileNameWithoutSuffix(paths[currentIndex]);
    loadImage();

    initComponents();
    calculateFileOutputPath();
    loadContent();
  }

  private void initComponents() {
    setLayout(new BorderLayout());

    // Create top, center and bottom panels
    JPanel topPanel = new JPanel(new BorderLayout());
    JPanel centerPanel = new JPanel(new GridLayout(1, 2));
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    JPanel topLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JPanel topMiddlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    topPanel.add(topLeftPanel, BorderLayout.WEST);
    topPanel.add(topMiddlePanel, BorderLayout.CENTER);
    topPanel.add(topRightPanel, BorderLayout.EAST);

    // Top panel components
    SpinnerModel model = new SpinnerNumberModel(3, 1, 8, 1);
    JSpinner zoomLevel = new JSpinner(model);
    saveButton = new JButton("Save");
    settingsButton = new JButton("Settings");
    JButton askButton = new JButton("Transcribe");
    bar = new JProgressBar();

    topLeftPanel.add(settingsButton);

    // Add rotate button to the top panel
    JButton rotateButton = new JButton("Rotate");
    topLeftPanel.add(rotateButton);
    rotateButton.addActionListener(e -> rotateImage());

    topMiddlePanel.add(zoomLevel);
    topMiddlePanel.add(askButton);
    topMiddlePanel.add(bar);
    topRightPanel.add(saveButton);

    // Center panel: image and text
    imageLabel = new JLabel();
    imageLabel.setHorizontalAlignment(JLabel.CENTER);
    imagePanel = new ImagePanel(imageLabel);
    centerPanel.add(imagePanel);

    textArea = new JTextArea();
    textArea.setEditable(true);
    JScrollPane textScrollPane = new JScrollPane(textArea);
    centerPanel.add(textScrollPane);

    // Bottom panel: navigation buttons
    prevButton = new JButton("Previous");
    nextButton = new JButton("Next");
    bottomPanel.add(prevButton);
    bottomPanel.add(nextButton);

    // Add panels to main panel
    add(topPanel, BorderLayout.NORTH);
    add(centerPanel, BorderLayout.CENTER);
    add(bottomPanel, BorderLayout.SOUTH);

    // Button listeners
    prevButton.addActionListener(e -> showPreviousImage());
    nextButton.addActionListener(e -> showNextImage());
    saveButton.addActionListener(e -> saveContent());
    askButton.addActionListener(e -> performTranscription(zoomLevel));

    // Resize listener with debouncing
    resizeTimer = new Timer(100, e -> updateDisplay());
    resizeTimer.setRepeats(false);
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        resizeTimer.restart();
      }
    });

    settingsButton.addActionListener(
        e -> {
          if (isUnsavedChanges()) {
            int option = JOptionPane.showConfirmDialog(this,
                "You have unsaved changes. Save them before moving to the previous image?",
                "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
            if (option == JOptionPane.CANCEL_OPTION) return;
            if (option == JOptionPane.YES_OPTION) saveContent();
          }
          switchToSettingsAction.run();
        }
    );

    updateDisplay();
  }

  public String getPrefKeyLastImageIndex() {
    return PREF_KEY_LAST_IMAGE_INDEX_BASE + ":" + outputFolder.toString().hashCode();
  }

  // New method to build the preference key for rotation based on outputFolder and file name.
  private String getRotationPrefKey() {
    return PREF_KEY_ROTATION_BASE + ":" + outputFolder.toString().hashCode() + ":" + getFileNameWithoutSuffix(paths[currentIndex]).hashCode();
  }

  private void performTranscription(JSpinner zoomLevel) {
    bar.setValue(5); // Dummy progress
    ProgressUpdateUtility progressUpdateUtility = new ProgressUpdateUtility((Integer) zoomLevel.getValue());
    var points = imagePanel.getOriginalCropCoordinates();
    BufferedImage croppedImage = CutImageUtil.cutImage(loadedImage, points);
    BufferedImage[] images = CutImageUtil.cutImage(croppedImage, (Integer) zoomLevel.getValue(), 50);

    Consumer<Float> listener = progress -> SwingUtilities.invokeLater(() ->
        bar.setValue((int) (progress * 100)));
    progressUpdateUtility.setListener(listener);

    JoinedTranscriber transcriber = new JoinedTranscriber(images, hints.language(), hints.story());
    transcriber.setProgressUpdateUtility(progressUpdateUtility);

    try {
      transcriber.transcribeImages(result -> {
        LineBreaker lineBreaker = new LineBreaker();
        textArea.setText(lineBreaker.lineBreakAfterEvery(result.text(), 100));
        progressUpdateUtility.removeListener(listener);
        bar.setValue(0);
      });
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void loadContent() {
    try {
      String content = fileHandler.loadFromFile(inputFileName);
      textArea.setText(content);
    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, "Failed to load file: " + e.getMessage(),
          "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void saveContent() {
    String content = textArea.getText();
    if (content.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Text area is empty. Nothing to save.",
          "Warning", JOptionPane.WARNING_MESSAGE);
      return;
    }
    try {
      fileHandler.saveToFile(inputFileName, content);
      JOptionPane.showMessageDialog(this, "File saved successfully.",
          "Success", JOptionPane.INFORMATION_MESSAGE);
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Failed to save file: " + ex.getMessage(),
          "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private boolean isUnsavedChanges() {
    try {
      String currentContent = textArea.getText();
      String savedContent = fileHandler.loadFromFile(inputFileName);
      return !currentContent.equals(savedContent);
    } catch (IOException e) {
      return true;
    }
  }

  private void showPreviousImage() {
    if (currentIndex > 0) {
      if (isUnsavedChanges()) {
        int option = JOptionPane.showConfirmDialog(this,
            "You have unsaved changes. Save them before moving to the previous image?",
            "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
        if (option == JOptionPane.CANCEL_OPTION) return;
        if (option == JOptionPane.YES_OPTION) saveContent();
      }
      currentIndex--;
      storeCurrentImageIndex(); // Save the updated index
      inputFileName = getFileNameWithoutSuffix(paths[currentIndex]);
      calculateFileOutputPath();
      loadContent();
      loadImage();
      imagePanel.resetCropRectangle();
      updateDisplay();
    }
  }

  private void showNextImage() {
    if (currentIndex < paths.length - 1) {
      if (isUnsavedChanges()) {
        int option = JOptionPane.showConfirmDialog(this,
            "You have unsaved changes. Save them before moving to the next image?",
            "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
        if (option == JOptionPane.CANCEL_OPTION) return;
        if (option == JOptionPane.YES_OPTION) saveContent();
      }
      currentIndex++;
      storeCurrentImageIndex(); // Save the updated index
      inputFileName = getFileNameWithoutSuffix(paths[currentIndex]);
      calculateFileOutputPath();
      loadContent();
      loadImage();
      imagePanel.resetCropRectangle();
      updateDisplay();
    }
  }

  private void storeCurrentImageIndex() {
    Preferences prefs = Preferences.userNodeForPackage(ImageContentPanel.class);
    prefs.putInt(getPrefKeyLastImageIndex(), currentIndex);
  }

  // Modified loadImage: reads the image from disk into originalImage,
  // then applies any stored rotation before assigning to loadedImage.
  private void loadImage() {
    try {
      originalImage = ImageIO.read(paths[currentIndex].toFile());
      Preferences prefs = Preferences.userNodeForPackage(ImageContentPanel.class);
      int rotation = prefs.getInt(getRotationPrefKey(), 0);
      loadedImage = RotateImageUtil.applyRotation(originalImage, rotation);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getFileNameWithoutSuffix(Path path) {
    String fileName = path.getFileName().toString();
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
  }

  private void calculateFileOutputPath() {
    fileTxtPath = outputFolder.resolve(currentIndex + ".txt");
  }

  private void updateDisplay() {
    if (paths.length == 0 || currentIndex < 0 || currentIndex >= paths.length) {
      return;
    }
    int labelWidth = imageLabel.getWidth();
    int labelHeight = imageLabel.getHeight();
    if (labelWidth <= 0 || labelHeight <= 0) return;

    int imgWidth = loadedImage.getWidth();
    int imgHeight = loadedImage.getHeight();
    double scale = Math.min((double) labelWidth / imgWidth, (double) labelHeight / imgHeight);
    int newWidth = (int) (imgWidth * scale);
    int newHeight = (int) (imgHeight * scale);

    Image scaledImg = resizeImage(loadedImage, newWidth, newHeight);
    imageLabel.setIcon(new ImageIcon(scaledImg));
    imageLabel.revalidate();
    imageLabel.repaint();

    prevButton.setEnabled(currentIndex > 0);
    nextButton.setEnabled(currentIndex < paths.length - 1);
    imagePanel.refreshIcon(loadedImage.getWidth(), loadedImage.getHeight());
  }

  private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
    BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = resizedImage.createGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
    g2d.dispose();
    return resizedImage;
  }

  // Called when the Rotate button is pressed.
  // Increments the rotation (modulo 4), saves it into preferences,
  // applies it on the in-memory image, and updates the display.
  private void rotateImage() {
    Preferences prefs = Preferences.userNodeForPackage(ImageContentPanel.class);
    int currentRotation = prefs.getInt(getRotationPrefKey(), 0);
    int newRotation = (currentRotation + 1) % 4;
    prefs.putInt(getRotationPrefKey(), newRotation);
    loadedImage = RotateImageUtil.applyRotation(originalImage, newRotation);
    updateDisplay();
  }
}