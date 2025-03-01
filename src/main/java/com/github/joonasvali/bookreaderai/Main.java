package com.github.joonasvali.bookreaderai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class Main {
  private static final String INPUT_FOLDER = "C:\\Users\\Joonas\\Desktop\\Paul\\1raamat";
  private static final String OUTPUT_FOLDER = "C:\\Users\\Joonas\\Desktop\\Paul\\1raamat\\transcription-output";
  private static final String[] ACCEPT_FILES = new String[]{"jpg"};

  public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
    OpenAIClient client = OpenAIOkHttpClient.fromEnv();

    SwingUtilities.invokeAndWait(() -> {
      MainFrame app = new MainFrame();
    });
  }

}