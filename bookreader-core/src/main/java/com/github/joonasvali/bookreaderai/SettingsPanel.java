package com.github.joonasvali.bookreaderai;

import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class SettingsPanel extends JPanel {
  private final Logger logger = org.slf4j.LoggerFactory.getLogger(SettingsPanel.class);

  public static final String LANGUAGE_KEY = "language";
  public static final String STORY_KEY = "story";

  private JLabel apiKeyStatusLabel;
  private JTextField folderPathField;
  private JButton chooseFolderButton;
  private JLabel folderErrorLabel;
  private JButton continueButton;

  private final TranscriptionHints defaultHints;

  // New fields
  private JTextField languageField;
  private JTextField storyField;

  private Preferences preferences;
  private final Consumer<Path> continueAction;

  public SettingsPanel(TranscriptionHints defaultHints, Consumer<Path> continueAction) {
    this.defaultHints = defaultHints;
    this.continueAction = continueAction;
    preferences = Preferences.userNodeForPackage(SettingsPanel.class);

    // Main layout: BorderLayout for easy separation of sections
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    // Top Panel: API Key Status
    JPanel topPanel = createApiKeyPanel();
    add(topPanel, BorderLayout.NORTH);

    // Center Panel: Folder Selection + Language/Topic fields
    JPanel centerPanel = createCenterPanel();
    add(centerPanel, BorderLayout.CENTER);

    // Bottom Panel: Continue Button
    JPanel bottomPanel = createBottomPanel();
    add(bottomPanel, BorderLayout.SOUTH);

    // Check if saved folder is valid; if so, enable "Continue" button
    String savedFolder = preferences.get("inputFolder", "");
    if (!savedFolder.isEmpty() && new File(savedFolder).isDirectory() && containsJpg(new File(savedFolder))) {
      continueButton.setEnabled(true);
    } else {
      continueButton.setEnabled(false);
      if (!savedFolder.isEmpty()) {
        folderErrorLabel.setText("Error: Saved folder is invalid or has no .jpg files.");
        folderErrorLabel.setForeground(Color.RED);
      }
    }
  }

  /**
   * Creates the top panel containing the API key status.
   */
  private JPanel createApiKeyPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    String openaiApiKey = System.getenv("OPENAI_API_KEY");
    if (openaiApiKey == null || openaiApiKey.isEmpty()) {
      logger.warn("OPENAI_API_KEY not found.");
      apiKeyStatusLabel = new JLabel("Error: OPENAI_API_KEY not found.");
      apiKeyStatusLabel.setForeground(Color.RED);
    } else {
      logger.debug("OPENAI_API_KEY loaded successfully.");
      apiKeyStatusLabel = new JLabel("OPENAI_API_KEY loaded successfully.");
      apiKeyStatusLabel.setForeground(Color.BLACK);
    }
    panel.add(apiKeyStatusLabel);
    return panel;
  }

  /**
   * Creates the center panel for folder selection, Language, and Topic,
   * using a GridBagLayout for a more controlled layout.
   */
  private JPanel createCenterPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Settings"));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(5, 5, 5, 5);

    // --- ROW 0: Folder label, text field, and button ---
    JLabel folderLabel = new JLabel("Input Folder:");
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.gridwidth = 1;
    panel.add(folderLabel, gbc);

    folderPathField = new JTextField(20);
    folderPathField.setEditable(false);
    folderPathField.setText(preferences.get("inputFolder", ""));
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 1;
    panel.add(folderPathField, gbc);

    chooseFolderButton = new JButton("Choose Folder");
    chooseFolderButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onChooseFolder();
      }
    });
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.weightx = 0;
    panel.add(chooseFolderButton, gbc);

    // --- ROW 1: Folder error label (spans all columns) ---
    folderErrorLabel = new JLabel("");
    folderErrorLabel.setForeground(Color.RED);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 3;
    gbc.weightx = 1;
    panel.add(folderErrorLabel, gbc);

    // Reset gridwidth for subsequent rows
    gbc.gridwidth = 1;

    // --- ROW 2: Language label and field ---
    JLabel languageLabel = new JLabel("Language:");
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 0;
    panel.add(languageLabel, gbc);

    languageField = new JTextField(20);
    languageField.setText(preferences.get(LANGUAGE_KEY, defaultHints.language()));
    // Save to preferences whenever text changes
    languageField.getDocument().addDocumentListener(new SimpleDocumentListener(() ->
        preferences.put(LANGUAGE_KEY, languageField.getText())
    ));
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.weightx = 1;
    gbc.gridwidth = 2;
    panel.add(languageField, gbc);

    // Reset gridwidth
    gbc.gridwidth = 1;

    // --- ROW 3: Topic label and field ---
    JLabel topicLabel = new JLabel("Story hint:");
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.weightx = 0;
    panel.add(topicLabel, gbc);

    storyField = new JTextField(20);
    storyField.setText(preferences.get(STORY_KEY, defaultHints.story()));
    // Save to preferences whenever text changes
    storyField.getDocument().addDocumentListener(new SimpleDocumentListener(() ->
        preferences.put(STORY_KEY, storyField.getText())
    ));
    gbc.gridx = 1;
    gbc.gridy = 3;
    gbc.weightx = 1;
    gbc.gridwidth = 2;
    panel.add(storyField, gbc);

    return panel;
  }

  /**
   * Creates the bottom panel containing the "Continue" button.
   */
  private JPanel createBottomPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    continueButton = new JButton("Continue");
    continueButton.addActionListener(e -> {
      continueAction.accept(Path.of(folderPathField.getText()));
    });
    continueButton.setEnabled(false);
    panel.add(continueButton);
    return panel;
  }

  /**
   * Opens a directory chooser and validates that the folder contains .jpg files.
   */
  private void onChooseFolder() {
    JFileChooser chooser = new JFileChooser(folderPathField.getText());
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int result = chooser.showOpenDialog(SettingsPanel.this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFolder = chooser.getSelectedFile();
      if (selectedFolder.isDirectory() && containsJpg(selectedFolder)) {
        logger.debug("Selected folder: {}", selectedFolder.getAbsolutePath());
        folderPathField.setText(selectedFolder.getAbsolutePath());
        folderErrorLabel.setText("");
        continueButton.setEnabled(true);
        // Save to preferences
        preferences.put("inputFolder", selectedFolder.getAbsolutePath());
      } else {
        logger.warn("Selected folder does not contain any .jpg files.");
        folderErrorLabel.setText("Error: Selected folder does not contain any .jpg files.");
        folderErrorLabel.setForeground(Color.RED);
        continueButton.setEnabled(false);
      }
    }
  }

  /**
   * Checks if the provided folder contains at least one file ending in .jpg or .jpeg.
   */
  private boolean containsJpg(File folder) {
    File[] jpgFiles = folder.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        String lowerName = name.toLowerCase();
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg");
      }
    });
    return jpgFiles != null && jpgFiles.length > 0;
  }

  public String getLanguage() {
    return languageField.getText();
  }

  public String getStory() {
    return storyField.getText();
  }

  /**
   * A small helper DocumentListener that invokes a Runnable whenever the text changes.
   */
  private static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
    private final Runnable onChange;

    public SimpleDocumentListener(Runnable onChange) {
      this.onChange = onChange;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
      onChange.run();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      onChange.run();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      onChange.run();
    }
  }
}
