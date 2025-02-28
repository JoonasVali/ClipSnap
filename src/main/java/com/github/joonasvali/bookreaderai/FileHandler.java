package com.github.joonasvali.bookreaderai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileHandler {
  private final String outputFolder;

  public FileHandler(String outputFolder) {
    this.outputFolder = outputFolder;
  }

  public Path getFilePath(String fileNameBody) {
    return Paths.get(outputFolder, fileNameBody + ".txt");
  }

  public void saveToFile(String fileNameBody, String content) throws IOException {
    System.out.println("Saving to file: " + fileNameBody);
    Path filePath = getFilePath(fileNameBody);
    Files.createDirectories(filePath.getParent());
    Files.writeString(filePath, content);
  }

  public String loadFromFile(String fileNameBody) throws IOException {
    System.out.println("Loading from file: " + fileNameBody);
    Path filePath = getFilePath(fileNameBody);
    if (Files.exists(filePath)) {
      return Files.readString(filePath);
    }
    return "";
  }
}