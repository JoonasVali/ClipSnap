package com.github.joonasvali.bookreaderai;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class MainFrame extends JFrame {
  public static final String[] ACCEPT_FILES = new String[] { "jpg" };

  private CardLayout cardLayout;
  private JPanel contentContainer;
  private SettingsPanel settingsPanel;

  public MainFrame() {
    setTitle("My Application");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setPreferredSize(new Dimension(1600, 1000));

    // Create container using CardLayout
    cardLayout = new CardLayout();
    contentContainer = new JPanel(cardLayout);

    // Create the settings panel with a callback that creates a new ImageContentPanel
    settingsPanel = new SettingsPanel((Path selectedFolder) -> {
      try {
        // List and sort image files from the selected folder
        Path[] imagePaths = sortByName(listInputFolderContent(selectedFolder));
        // Determine output folder for transcriptions
        Path outputFolder = selectedFolder.resolve("transcription-output");
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
    contentContainer.add(settingsPanel, "SETTINGS_PANEL");

    // Add the container to the frame
    getContentPane().add(contentContainer);

    pack();
    setLocationRelativeTo(null);
    setVisible(true);
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
    contentContainer.add(panel, "IMAGE_PANEL");
    contentContainer.revalidate();
    contentContainer.repaint();
    // Show the image panel card
    cardLayout.show(contentContainer, "IMAGE_PANEL");
  }

  /**
   * Switches back to the settings panel.
   */
  public void switchPanelToSettingPanel() {
    cardLayout.show(contentContainer, "SETTINGS_PANEL");
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