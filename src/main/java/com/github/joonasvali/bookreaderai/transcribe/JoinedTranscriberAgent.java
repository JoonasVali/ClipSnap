package com.github.joonasvali.bookreaderai.transcribe;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class JoinedTranscriberAgent {
  private final BufferedImage[] images;
  private final String language;
  private final String story;

  public JoinedTranscriberAgent(BufferedImage[] images, String language, String story) {
    this.images = images;
    this.language = language;
    this.story = story;
  }

  private void transcribeImages(BiConsumer<Integer, String> callback) throws IOException {
    SimpleTranscriberAgent[] agents = new SimpleTranscriberAgent[images.length];
    for (int i = 0; i < images.length; i++) {
      agents[i] = new SimpleTranscriberAgent(images[i], language, story);
    }

    for (int i = 0; i < images.length; i++) {
      int finalI = i;
      agents[i].transcribe().thenAccept(text -> callback.accept(finalI, text));
    }
  }

  public void transcribeImages(Consumer<String> callback) throws IOException {
    String[] results = new String[images.length];
    Arrays.fill(results, "...");

    transcribeImages((index, result) -> {
      results[index] = result;
      synchronized (callback) {
        StringBuilder builder = new StringBuilder();
        for (String s : results) {
          builder.append(s);
          builder.append("\n");
        }
        callback.accept(builder.toString());
      }
    });
  }
}
