package com.github.joonasvali.bookreaderai;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Main {
  public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
    Properties properties = new Properties();
    Path path = Path.of("snapread.properties").toAbsolutePath();
    try (var in = Files.newInputStream(path)) {
      properties.load(in);
    }

    SwingUtilities.invokeAndWait(() -> {
      MainFrame app = new MainFrame(properties);
    });
  }
}