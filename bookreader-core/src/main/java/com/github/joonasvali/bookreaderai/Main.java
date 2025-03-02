package com.github.joonasvali.bookreaderai;

import org.slf4j.Logger;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Main {
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Main.class);
  public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
    Properties properties = new Properties();
    Path path = Path.of("snapread.properties").toAbsolutePath();
    try (var in = Files.newInputStream(path)) {
      properties.load(in);
    }
    logger.debug("Properties file: {}", path);
    logger.debug("Properties loaded: {}", properties);

    SwingUtilities.invokeAndWait(() -> new MainFrame(properties));
  }
}