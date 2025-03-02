package com.github.joonasvali.bookreaderai;

import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

public class MainFrame extends JFrame {
  public static final int MAX_WIDTH = 1600;
  public static final int MAX_HEIGHT = 1000;
  private final Logger logger = org.slf4j.LoggerFactory.getLogger(MainFrame.class);

  public static final String TITLE = "ClipSnap by Joonas Vali, 2025";
  public static final String[] ACCEPT_FILES = new String[]{"jpg"};
  public static final String SETTINGS_PANEL_KEY = "SETTINGS_PANEL";
  public static final String IMAGE_PANEL_KEY = "IMAGE_PANEL";
  public static final String TRANSCRIPTION_OUTPUT_FOLDER = "transcription-output";

  private CardLayout cardLayout;
  private JPanel contentContainer;
  private SettingsPanel settingsPanel;


  public MainFrame(Properties properties) {
    setTitle(TITLE);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    adjustToDefaultFrameSize();

    // Create container using CardLayout
    cardLayout = new CardLayout();
    contentContainer = new JPanel(cardLayout);

    // Create the settings panel with a callback that creates a new ImageContentPanel
    settingsPanel = new SettingsPanel(
        new TranscriptionHints(
            properties.getProperty("default.hint.language"),
            properties.getProperty("default.hint.story")
        ), (Path selectedFolder) -> {
      try {
        // List and sort image files from the selected folder
        Path[] imagePaths = sortByName(listInputFolderContent(selectedFolder));
        // Determine output folder for transcriptions
        Path outputFolder = selectedFolder.resolve(TRANSCRIPTION_OUTPUT_FOLDER);
        TranscriptionHints hints = new TranscriptionHints(
            settingsPanel.getLanguage().trim().isEmpty() ? null : settingsPanel.getLanguage(),
            settingsPanel.getStory()
        );
        // Create a new image panel
        ImageContentPanel imagePanel = new ImageContentPanel(hints, imagePaths, outputFolder, this::switchPanelToSettingPanel);
        // Switch to the new image panel
        switchPanelToImagePanel(imagePanel);
      } catch (IOException e) {
        JOptionPane.showMessageDialog(
            MainFrame.this,
            e.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
      }
    });

    // Add the settings panel to the container
    contentContainer.add(settingsPanel, SETTINGS_PANEL_KEY);

    // Add the container to the frame
    getContentPane().add(contentContainer);

    pack();
    setLocationRelativeTo(null);
    setVisible(true);
  }

  private void adjustToDefaultFrameSize() {
    // Get the current screen dimensions.
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    // Define a comfortable factor (90% of screen size) and maximum values.
    double comfortableFactor = 0.9;

    // Calculate dimensions as 90% of the screen dimensions.
    int width = (int)(screenSize.width * comfortableFactor);
    int height = (int)(screenSize.height * comfortableFactor);

    // Ensure the size does not exceed your preferred maximum.
    width = Math.min(width, MAX_WIDTH);
    height = Math.min(height, MAX_HEIGHT);

    // Set the calculated preferred size.
    setPreferredSize(new Dimension(width, height));
  }

  /**
   * Switches to a newly created ImageContentPanel.
   * This method removes any previous image panel so that the ImageContentPanel
   * is recreated each time it is accessed from settings.
   *
   * @param panel the new ImageContentPanel instance to show
   */
  public void switchPanelToImagePanel(ImageContentPanel panel) {
    // Remove any existing ImageContentPanel
    for (Component comp : contentContainer.getComponents()) {
      if (comp instanceof ImageContentPanel) {
        contentContainer.remove(comp);
        break;
      }
    }
    // Add the new image panel with a specific name
    contentContainer.add(panel, IMAGE_PANEL_KEY);
    contentContainer.revalidate();
    contentContainer.repaint();
    // Show the image panel card
    cardLayout.show(contentContainer, IMAGE_PANEL_KEY);
  }

  /**
   * Switches back to the settings panel.
   */
  public void switchPanelToSettingPanel() {
    cardLayout.show(contentContainer, SETTINGS_PANEL_KEY);
  }

  /**
   * Lists the input folder contents filtered by ACCEPT_FILES.
   *
   * @param folder the folder to list
   * @return an array of Path objects for accepted files
   * @throws IOException if file walking fails
   */
  public static Path[] listInputFolderContent(Path folder) throws IOException {
    try (var stream = Files.walk(folder)) {
      return stream.filter(Files::isRegularFile)
          .filter(path -> {
            for (String acceptFile : ACCEPT_FILES) {
              if (path.toString().endsWith(acceptFile)) {
                return true;
              }
            }
            return false;
          })
          .toArray(Path[]::new);
    }
  }

  /**
   * Sorts an array of Path objects by their natural ordering (i.e. by name).
   *
   * @param paths the unsorted array of paths
   * @return a sorted array of paths
   */
  public static Path[] sortByName(Path[] paths) {
    return Arrays.stream(paths)
        .filter(Files::isRegularFile)
        .sorted()
        .toArray(Path[]::new);
  }
}