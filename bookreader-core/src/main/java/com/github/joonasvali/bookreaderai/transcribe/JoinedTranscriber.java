package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.ProgressUpdateUtility;
import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.textutil.TextJoiner;
import com.github.joonasvali.bookreaderai.textutil.textjoiner.TextJoinerAI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public class JoinedTranscriber {
  private static final Logger logger = LoggerFactory.getLogger(JoinedTranscriber.class);

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

    // This list will hold each transcription's content.
    List<String> resultsList = new ArrayList<>();

    // Start the chain with an initial dummy result.
    CompletableFuture<ProcessingResult<String>> chain = CompletableFuture.completedFuture(
        new ProcessingResult<>(null, 0, 0, 0)
    );

    for (int i = 0; i < agents.length; i++) {
      final int index = i;
      // Chain each transcription sequentially.
      chain = chain.thenCompose(previousResult ->
          agents[index].transcribe(previousResult.content()).thenApply(result -> {
            // Save the result's content into the list.
            resultsList.add(result.content());
            if (progressUpdateUtility != null) {
              progressUpdateUtility.setTranscribeTaskComplete(index, true);
            }
            // Return the result to be used as input for the next agent.
            return result;
          })
      );
    }

    // Once all tasks have run sequentially, join the results and send the final callback.
    chain.thenAccept(finalResult -> {
      ProcessingResult<String> joinedResult = join(resultsList);
      if (progressUpdateUtility != null) {
        progressUpdateUtility.setFinalTaskComplete();
      }
      callback.accept(joinedResult);
    });
  }
  private ProcessingResult<String> join(List<String> results) {
    if (results.size() == 1) {
      return new ProcessingResult<>(results.getFirst(), 0, 0, 0);
    }
    logger.debug("Joining {} texts", results.size());
    long totalTokens = 0;
    long promptTokens = 0;
    long completionTokens = 0;

    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < results.size(); i++) {
      stringBuilder.append("\n").append(results.get(i));
    }

    return new ProcessingResult<>(stringBuilder.toString(), promptTokens, completionTokens, totalTokens);
  }

  public void setProgressUpdateUtility(ProgressUpdateUtility progressUpdateUtility) {
    this.progressUpdateUtility = progressUpdateUtility;
  }
}
