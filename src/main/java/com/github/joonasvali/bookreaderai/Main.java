package com.github.joonasvali.bookreaderai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatModel;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.github.joonasvali.bookreaderai.ImageLoader.loadImagesFromPaths;

public class Main {
  private static final String INPUT_FOLDER = "C:\\Users\\Joonas\\Desktop\\Paul\\test";
  private static final String OUTPUT_FOLDER = "C:\\Users\\Joonas\\Desktop\\Paul\\test\\transcription-output";
  private static final String[] ACCEPT_FILES = new String[]{"jpg"};

  public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
    OpenAIClient client = OpenAIOkHttpClient.fromEnv();

    Path[] inputFiles = listInputFolderContent();

    SwingUtilities.invokeAndWait(() -> {
      ApplicationUI app = new ApplicationUI(inputFiles);

      app.setVisible(true);
    });


//    ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
//        .addUserMessage("Say this is a test")
//        .model(ChatModel.CHATGPT_4O_LATEST)
//        .build();
//    ChatCompletion chatCompletion = client.chat().completions().create(params);
//    System.out.println(chatCompletion.choices().getFirst().message().content().orElse("-No content-"));
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