package com.github.joonasvali.bookreaderai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.*;

public class FinalResultManager {
  private final Logger logger = LoggerFactory.getLogger(FinalResultManager.class);
  Path[] imagePaths;
  FileHandler fileHandler;

  public FinalResultManager(Path[] imagePaths, FileHandler fileHandler) {
    this.fileHandler = fileHandler;
    this.imagePaths = imagePaths;
  }

  public void invokeSaveFinalResultDialog(JComponent parent) {
    Preferences prefs = Preferences.userNodeForPackage(FinalResultManager.class);
    // Use the hash code of the output folder's string representation as key
    String key = fileHandler.getOutputFolder().toString().hashCode() + "";
    String value = prefs.get(key, null);

    // Create a file chooser and, if a previous value exists, pre-select that file
    JFileChooser fileChooser = new JFileChooser();
    if (value != null) {
      fileChooser.setSelectedFile(new File(value));
    }

    File fileToSave;
    // Show the save dialog allowing the user to choose or change the file location
    int userSelection = fileChooser.showSaveDialog(parent);
    if (userSelection == JFileChooser.APPROVE_OPTION) {
      fileToSave = fileChooser.getSelectedFile();
      prefs.put(key, fileToSave.getAbsolutePath());
      logger.info("Saving final result to: {}", fileToSave.getAbsolutePath());
    } else {
      logger.info("Save command cancelled by user.");
      return;
    }

    // Aggregate the content from each output file.
    StringBuilder strb = new StringBuilder();
    List<Path> filesNotTranscribed = new ArrayList<>();
    for (Path imagePath : imagePaths) {
      Path transcriptionPath = fileHandler.getOutputFilePath(imagePath);
      if (!Files.exists(transcriptionPath)) {
        logger.warn("File not found: " + transcriptionPath.toAbsolutePath());
        filesNotTranscribed.add(imagePath);
        continue;
      }
      try {
        // Read the content from the file corresponding to the image
        String content = new String(Files.readAllBytes(transcriptionPath));
        strb.append(content).append(System.lineSeparator());
      } catch (IOException e) {
        logger.error("Failed to read file: " + transcriptionPath.toAbsolutePath(), e);
      }
    }

    // If there are files not transcribed, prompt the user with a cowboyish confirmation dialog.
    if (!filesNotTranscribed.isEmpty()) {
      int transcribedCount = imagePaths.length - filesNotTranscribed.size();
      int totalCount = imagePaths.length;
      String cowboyMessage = "Yeehaw, partner! Looks like only " + transcribedCount +
          " outta " + totalCount + " files got transcribed. " +
          "Are ya sure ya wanna ride on and save what's been done?";
      int option = JOptionPane.showConfirmDialog(parent, cowboyMessage, "Confirm Save", JOptionPane.YES_NO_OPTION);
      if (option != JOptionPane.YES_OPTION) {
        logger.info("User cancelled saving final result after cowboy prompt.");
        return;
      }
    }

    if (Files.exists(fileToSave.toPath())) {
      int option = JOptionPane.showConfirmDialog(parent, "File already exists. Overwrite?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
      if (option != JOptionPane.YES_OPTION) {
        logger.info("User cancelled saving final result after overwrite prompt.");
        return;
      }
    }

    // Save the final result to the selected file.
    try {
      Files.write(fileToSave.toPath(), strb.toString().getBytes());
      logger.info("Final result successfully saved.");
      JOptionPane.showMessageDialog(parent, "Successfully saved final result to: " + fileToSave.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
    } catch (IOException e) {
      logger.error("Failed to save final result to: " + fileToSave.getAbsolutePath(), e);
    }
  }
}
