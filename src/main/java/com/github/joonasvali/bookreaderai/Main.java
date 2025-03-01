package com.github.joonasvali.bookreaderai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
  private static final String INPUT_FOLDER = "C:\\Users\\Joonas\\Desktop\\Paul\\1raamat";
  private static final String OUTPUT_FOLDER = "C:\\Users\\Joonas\\Desktop\\Paul\\1raamat\\transcription-output";
  private static final String[] ACCEPT_FILES = new String[]{"jpg"};

  public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
    OpenAIClient client = OpenAIOkHttpClient.fromEnv();

    Path[] inputFiles = sortByName(listInputFolderContent());

    SwingUtilities.invokeAndWait(() -> {
      ApplicationUI app = new ApplicationUI(inputFiles, OUTPUT_FOLDER);

      app.setVisible(true);
    });
  }

  public static Path[] listInputFolderContent() throws IOException {
    try (var stream = Files.walk(Paths.get(INPUT_FOLDER))) {
      return stream.filter(Files::isRegularFile).
          filter(path -> {
            for (String acceptFile : ACCEPT_FILES) {
              if (path.toString().endsWith(acceptFile)) {
                return true;
              }
            }
            return false;
          }).toArray(Path[]::new);
    }
  }

  public static Path[] sortByName(Path[] paths) throws IOException {
    return Files.walk(Paths.get(INPUT_FOLDER)).filter(Files::isRegularFile).sorted().toArray(Path[]::new);
  }
}