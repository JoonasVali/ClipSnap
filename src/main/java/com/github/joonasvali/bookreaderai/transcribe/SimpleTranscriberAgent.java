package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.openai.ImageAnalysis;
import com.github.joonasvali.bookreaderai.openai.ImageAnalysisResult;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class SimpleTranscriberAgent {
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


  public SimpleTranscriberAgent(BufferedImage bufferedImage, String language, String story) {
    this.bufferedImage = bufferedImage;
    if (language != null) {
      this.languageDirection = "The text is in " + language + " mostly.";
    } else {
      this.languageDirection = "";
    }
    this.story = story;
  }

  public CompletableFuture<String> transcribe() throws IOException {
    return CompletableFuture.supplyAsync(() -> {
      try {
        ImageAnalysis imageAnalysis = new ImageAnalysis(bufferedImage, SYSTEM_PROMPT
            .replace("${LANGUAGE}", languageDirection)
            .replace("${STORY}", story)
        );
        ImageAnalysisResult<String> result = imageAnalysis.process(bufferedImage);
        System.out.println("Prompt tokens: " + result.promptTokens());
        System.out.println("Completion tokens: " + result.completionTokens());
        System.out.println("Total tokens: " + result.totalTokens());
        return result.text();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

}
