package com.github.joonasvali.bookreaderai;

import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class SettingsPanel extends JPanel {
  public static final String INPUT_FOLDER_KEY = "inputFolder";
  public static final int CONTENT_WIDTH = 800;
  private final Logger logger = org.slf4j.LoggerFactory.getLogger(SettingsPanel.class);

  public static final String LANGUAGE_KEY = "language";
  public static final String STORY_KEY = "story";
  public static final String GPT_MODEL_KEY = "gptModel";

  private JLabel apiKeyStatusLabel;
  private JTextField folderPathField;
  private JButton chooseFolderButton;
  private JLabel folderErrorLabel;
  private JButton continueButton;

  private final TranscriptionHints defaultHints;

  // New fields
  private JTextField languageField;
  private JTextField storyField;
  private JComboBox<String> gptModelComboBox;

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
    String savedFolder = preferences.get(INPUT_FOLDER_KEY, "");
    if (!savedFolder.isEmpty() && new File(savedFolder).isDirectory() && containsImage(new File(savedFolder))) {
      continueButton.setEnabled(true);
    } else {
      continueButton.setEnabled(false);
      if (!savedFolder.isEmpty()) {
        folderErrorLabel.setText("Error: Saved folder is invalid or has no .jpg or .png files.");
        folderErrorLabel.setForeground(Color.RED);
      }
    }
  }

  /**
   * Creates the top panel containing the API key status and GPT model selection.
   */
  private JPanel createApiKeyPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    
    // Left side: API Key Status
    JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    String openaiApiKey = System.getenv(Constants.OPENAI_API_KEY_ENV_VARIABLE);
    if (openaiApiKey == null || openaiApiKey.isEmpty()) {
      logger.warn(Constants.OPENAI_API_KEY_ENV_VARIABLE + " not found.");
      apiKeyStatusLabel = new JLabel("Error: " + Constants.OPENAI_API_KEY_ENV_VARIABLE + " not found.");
      apiKeyStatusLabel.setForeground(Color.RED);
    } else {
      logger.debug(Constants.OPENAI_API_KEY_ENV_VARIABLE + " loaded successfully.");
      apiKeyStatusLabel = new JLabel(Constants.OPENAI_API_KEY_ENV_VARIABLE + " loaded successfully.");
      apiKeyStatusLabel.setForeground(Color.BLACK);
    }
    leftPanel.add(apiKeyStatusLabel);
    
    // Right side: GPT Model Selection
    JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JLabel modelLabel = new JLabel("GPT Model:");
    rightPanel.add(modelLabel);
    
    String[] models = {"GPT-4.1", "GPT-5.2"};
    gptModelComboBox = new JComboBox<>(models);
    gptModelComboBox.setSelectedItem(preferences.get(GPT_MODEL_KEY, "GPT-4.1"));
    gptModelComboBox.addActionListener(e -> {
      preferences.put(GPT_MODEL_KEY, (String) gptModelComboBox.getSelectedItem());
    });
    rightPanel.add(gptModelComboBox);
    
    panel.add(leftPanel, BorderLayout.WEST);
    panel.add(rightPanel, BorderLayout.EAST);
    return panel;
  }

  /**
   * Creates the center panel for folder selection, Language, and Topic.
   * The outer panel ("Settings") is full-width, while the inner content
   * is forced to exactly 800px wide and centered at the top.
   */
  private JPanel createCenterPanel() {
    // The main outer panel that spans the full width, with a titled border
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(BorderFactory.createTitledBorder("Settings"));

    // The content panel that will actually hold the labels/fields
    JPanel contentPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(5, 5, 5, 5);

    // --- ROW 0: Folder label, text field, and button ---
    JLabel folderLabel = new JLabel("Input Folder:");
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    gbc.gridwidth = 1;
    contentPanel.add(folderLabel, gbc);

    folderPathField = new JTextField(20);
    folderPathField.setEditable(false);
    folderPathField.setText(preferences.get(INPUT_FOLDER_KEY, ""));
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 1;
    contentPanel.add(folderPathField, gbc);

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
    contentPanel.add(chooseFolderButton, gbc);

    // --- ROW 1: Folder error label (spans all columns) ---
    folderErrorLabel = new JLabel("");
    folderErrorLabel.setForeground(Color.RED);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 3;
    gbc.weightx = 1;
    contentPanel.add(folderErrorLabel, gbc);

    // Reset gridwidth for subsequent rows
    gbc.gridwidth = 1;

    // --- ROW 2: Language label and field ---
    JLabel languageLabel = new JLabel("Language:");
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 0;
    contentPanel.add(languageLabel, gbc);

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
    contentPanel.add(languageField, gbc);

    // Reset gridwidth
    gbc.gridwidth = 1;

    // --- ROW 3: Topic label and field ---
    JLabel topicLabel = new JLabel("Story hint:");
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.weightx = 0;
    contentPanel.add(topicLabel, gbc);

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
    contentPanel.add(storyField, gbc);

    /*
     * Force the content panel to always be exactly 800px wide:
     *   - preferredSize sets the default "wanted" size
     *   - minimumSize prevents it from shrinking below 800px
     *   - maximumSize prevents it from growing beyond 800px
     */
    Dimension fixedSize = new Dimension(CONTENT_WIDTH, contentPanel.getPreferredSize().height);
    contentPanel.setPreferredSize(fixedSize);
    contentPanel.setMinimumSize(fixedSize);
    contentPanel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

    // Flow panel to center the content horizontally
    JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    flowPanel.add(contentPanel);

    // Add the flow panel to the top of the main panel
    mainPanel.add(flowPanel, BorderLayout.NORTH);

    return mainPanel;
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
   * Opens a directory chooser and validates that the folder contains .jpg or .png files.
   */
  private void onChooseFolder() {
    JFileChooser chooser = new JFileChooser(folderPathField.getText());
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int result = chooser.showOpenDialog(SettingsPanel.this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFolder = chooser.getSelectedFile();
      if (selectedFolder.isDirectory() && containsImage(selectedFolder)) {
        logger.debug("Selected folder: {}", selectedFolder.getAbsolutePath());
        folderPathField.setText(selectedFolder.getAbsolutePath());
        folderErrorLabel.setText("");
        continueButton.setEnabled(true);
        // Save to preferences
        preferences.put(INPUT_FOLDER_KEY, selectedFolder.getAbsolutePath());
      } else {
        logger.warn("Selected folder does not contain any .jpg or .png files.");
        folderErrorLabel.setText("Error: Selected folder does not contain any .jpg or .png files.");
        folderErrorLabel.setForeground(Color.RED);
        continueButton.setEnabled(false);
      }
    }
  }

  /**
   * Checks if the provided folder contains at least one file ending in .png, .jpg or .jpeg.
   */
  private boolean containsImage(File folder) {
    File[] imageFiles = folder.listFiles((dir, name) -> {
      String lowerName = name.toLowerCase();
      return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png");
    });
    return imageFiles != null && imageFiles.length > 0;
  }

  public String getLanguage() {
    return languageField.getText();
  }

  public String getStory() {
    return storyField.getText();
  }

  public String getGptModel() {
    return (String) gptModelComboBox.getSelectedItem();
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
