package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.ProgressUpdateUtility;
import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.textutil.TextJoiner;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public class JoinedTranscriber {


  private final BufferedImage[] images;
  private final String language;
  private final String story;
  private ProgressUpdateUtility progressUpdateUtility;

  public JoinedTranscriber(BufferedImage[] images, String language, String story) {
    this.images = images;
    this.language = language;
    this.story = story;
  }

  public void transcribeImages(Consumer<ProcessingResult<String>> callback) throws IOException {
    SimpleTranscriberAgent[] agents = new SimpleTranscriberAgent[images.length];
    for (int i = 0; i < images.length; i++) {
      agents[i] = new SimpleTranscriberAgent(images[i], language, story);
    }


    List<CompletableFuture<ProcessingResult<String>>> futures = new ArrayList<>();
    for (int i = 0; i < agents.length; i++) {
      int index = i;
      CompletableFuture<ProcessingResult<String>> future = agents[i].transcribe();
      if (progressUpdateUtility != null) {
        future.thenRun(() -> progressUpdateUtility.setTranscribeTaskComplete(index, true));
      }
      futures.add(future);
    }

    CompletableFuture<Void> allDone = CompletableFuture.allOf(
        futures.toArray(new CompletableFuture[0])
    );

    allDone.thenRun(() -> {
      // All tasks finished. Gather results:
      List<ProcessingResult<String>> results = futures.stream().map(future -> {
        try {
          return future.join();
        } catch (CompletionException e) {
          e.printStackTrace();
          return new ProcessingResult<>("Task failed", 0, 0, 0);
        }
      }).toList();

      if (progressUpdateUtility != null) {
        progressUpdateUtility.setFinalTaskComplete();
      }

      List<String> texts = new ArrayList<>();
      for (ProcessingResult<String> result : results) {
        texts.add(result.text());
      }

      callback.accept(new ProcessingResult<>(join(texts),
          results.stream().mapToLong(ProcessingResult::promptTokens).sum(),
          results.stream().mapToLong(ProcessingResult::completionTokens).sum(),
          results.stream().mapToLong(ProcessingResult::totalTokens).sum()
      ));
    });

  }

  private String join(List<String> results) {
    TextJoiner joiner = new TextJoiner();
    String joinedText = results.get(0);

    for (int i = 1; i < results.size(); i++) {
      joinedText = joiner.join(joinedText, results.get(i));
    }

    return joinedText;
  }

  public void setProgressUpdateUtility(ProgressUpdateUtility progressUpdateUtility) {
    this.progressUpdateUtility = progressUpdateUtility;
  }
}
