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

  public Path getFilePath(int index) {
    return Paths.get(outputFolder, index + ".txt");
  }

  public void saveToFile(int index, String content) throws IOException {
    Path filePath = getFilePath(index);
    Files.createDirectories(filePath.getParent());
    Files.writeString(filePath, content);
  }

  public String loadFromFile(int index) throws IOException {
    Path filePath = getFilePath(index);
    if (Files.exists(filePath)) {
      return Files.readString(filePath);
    }
    return "";
  }
}