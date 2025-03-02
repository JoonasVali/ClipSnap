package com.github.joonasvali.bookreaderai;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHandler {
  private final Logger logger = org.slf4j.LoggerFactory.getLogger(FileHandler.class);
  private final Path outputFolder;

  public FileHandler(Path outputFolder) {
    this.outputFolder = outputFolder;
  }

  public Path getOutputFilePath(Path inputFilePath) {
    return getOutputFilePath(getFileNameWithoutSuffix(inputFilePath));
  }

  public Path getOutputFilePath(String fileNameBody) {
    return outputFolder.resolve(fileNameBody + ".txt");
  }

  public void saveToFile(String fileNameBody, String content) throws IOException {
    logger.info("Saving to file: {}", fileNameBody);
    Path filePath = getOutputFilePath(fileNameBody);
    Files.createDirectories(filePath.getParent());
    Files.writeString(filePath, content);
  }

  public String loadFromFile(String fileNameBody) throws IOException {
    logger.info("Loading from file: {}", fileNameBody);
    Path filePath = getOutputFilePath(fileNameBody);
    if (Files.exists(filePath)) {
      return Files.readString(filePath);
    }
    return "";
  }

  public static String getFileNameWithoutSuffix(Path path) {
    String fileName = path.getFileName().toString();
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
  }

  public Path getOutputFolder() {
    return outputFolder;
  }
}