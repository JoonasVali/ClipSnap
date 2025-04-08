package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.ProgressUpdateUtility;
import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    List<ProcessingResult<String>> resultsList = new ArrayList<>();

    // Start with an initial dummy result.
    ProcessingResult<String> previousResult = new ProcessingResult<>(null, 0, 0, 0);

    for (int i = 0; i < agents.length; i++) {
      // Perform each transcription synchronously.
      ProcessingResult<String> result = agents[i].transcribe(previousResult.content());
      resultsList.add(result);

      if (progressUpdateUtility != null) {
        progressUpdateUtility.setTranscribeTaskComplete(i, true);
      }

      // Update previousResult to pass its content to the next agent.
      previousResult = result;
    }

    // Combine the results and send the final callback.
    ProcessingResult<String> joinedResult = join(resultsList);
    if (progressUpdateUtility != null) {
      progressUpdateUtility.setFinalTaskComplete();
    }
    callback.accept(joinedResult);
  }

  private ProcessingResult<String> join(List<ProcessingResult<String>> results) {
    if (results.size() == 1) {
      return new ProcessingResult<>(results.getFirst().content(), results.getFirst().promptTokens(), results.getFirst().completionTokens(), results.getFirst().totalTokens());
    }
    logger.debug("Joining {} texts", results.size());
    long totalTokens = 0;
    long promptTokens = 0;
    long completionTokens = 0;

    StringBuilder stringBuilder = new StringBuilder();
    for (ProcessingResult<String> result : results) {
      stringBuilder.append(result.content());
      totalTokens += result.totalTokens();
      promptTokens += result.promptTokens();
      completionTokens += result.completionTokens();
    }

    return new ProcessingResult<>(stringBuilder.toString(), promptTokens, completionTokens, totalTokens);
  }

  public void setProgressUpdateUtility(ProgressUpdateUtility progressUpdateUtility) {
    this.progressUpdateUtility = progressUpdateUtility;
  }
}
