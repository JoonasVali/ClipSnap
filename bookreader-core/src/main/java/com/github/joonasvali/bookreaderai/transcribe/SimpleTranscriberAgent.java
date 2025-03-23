package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.openai.ImageAnalysis;
import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class SimpleTranscriberAgent {
  private static final Logger logger = LoggerFactory.getLogger(SimpleTranscriberAgent.class);

  private static final String SYSTEM_PROMPT = """
        You are a professional Transcriber. Transcribe image and give the text back without explanation.
        ${LANGUAGE}Make your best judgement to detect the words in the picture if they
        are damaged. You are smart and can detect missing pieces from context as well.
        ${STORY}Avoid adding asterisks or format unless they are part of the text. Only output the
        transcribed text, nothing else. Remember, do not write explanations or meta comments. Good luck!
      """;

  private final String languageDirection;
  private final BufferedImage bufferedImage;
  private final String story;
  private boolean useFixerAgent;
  private final String language;

  public SimpleTranscriberAgent(BufferedImage bufferedImage, String language, String story) {
    this(bufferedImage, language, story, false);
  }

  public SimpleTranscriberAgent(BufferedImage bufferedImage, String language, String story, boolean useFixerAgent) {
    this.bufferedImage = bufferedImage;
    if (language != null) {
      this.languageDirection = "The content is in " + language + " mostly.";
    } else {
      this.languageDirection = "";
    }
    this.story = story + " ";
    this.useFixerAgent = useFixerAgent;
    this.language = language;
  }

  public CompletableFuture<ProcessingResult<String>> transcribe() {
    ImageAnalysis imageAnalysis = createImageAnalysis();
    return CompletableFuture.supplyAsync(() -> {
      try {
        ProcessingResult<String[]> results = processImage(imageAnalysis);

        TranscriptionVerifierAgent verifierAgent = new TranscriptionVerifierAgent(language, story);
        ProcessingResult<String> result = verifierAgent.verify(results.content());

        if (useFixerAgent) {
          result = fixTranscriptionResult(result);
          return new ProcessingResult<>(result.content(),
              result.promptTokens() + results.promptTokens(),
              result.completionTokens() + results.completionTokens(),
              result.totalTokens() + results.totalTokens()
          );
        } else {
          return new ProcessingResult<>(result.content(),
              result.promptTokens() + results.promptTokens(),
              result.completionTokens() + results.completionTokens(),
              result.totalTokens() + results.totalTokens()
          );
        }
      } catch (Exception e) {
        logger.error("Unable to complete transcription", e);
        throw new RuntimeException(e);
      }
    });
  }

  private ImageAnalysis createImageAnalysis() {
    return new ImageAnalysis(SYSTEM_PROMPT
        .replace("${LANGUAGE}", languageDirection)
        .replace("${STORY}", story)
    );
  }

  private ProcessingResult<String[]> processImage(ImageAnalysis imageAnalysis) throws IOException {
    return imageAnalysis.process(bufferedImage, 3);
  }

  private ProcessingResult<String> fixTranscriptionResult(ProcessingResult<String> result) {
    TranscribeFixerAgent fixerAgent = new TranscribeFixerAgent(language, story);
    ProcessingResult<String> fixedResult = fixerAgent.fix(result.content());

    return new ProcessingResult<>(
        fixedResult.content(),
        result.promptTokens() + fixedResult.promptTokens(),
        result.completionTokens() + fixedResult.completionTokens(),
        result.totalTokens() + fixedResult.totalTokens()
    );
  }
}
