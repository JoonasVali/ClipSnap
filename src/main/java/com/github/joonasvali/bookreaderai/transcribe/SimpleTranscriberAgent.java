package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.openai.ImageProcess;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class SimpleTranscriberAgent {
  private static final String SYSTEM_PROMPT = """
        You are a professional Transcriber. Transcribe image and give the text back without explanation.
        The text is in Estonian mostly. Make your best judgement to detect the words in the picture if they
        are damaged. You are smart and can detect missing pieces from context as well. This story is historical
        from around ww2. Avoid adding asterisks or format unless they are part of the text. Only output the
        transcribed text, nothing else. Remember, do not write explanations or meta comments. Good luck!
      """;
  private final BufferedImage bufferedImage;

  public SimpleTranscriberAgent(BufferedImage bufferedImage) {
    this.bufferedImage = bufferedImage;
  }

  public CompletableFuture<String> transcribe() throws IOException {
    return CompletableFuture.supplyAsync(() -> {
      try {
        ImageProcess imageProcess = new ImageProcess(bufferedImage, SYSTEM_PROMPT);
        return imageProcess.process(bufferedImage);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

}
