package com.github.joonasvali.bookreaderai.transcribe;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

public class Transcriber {
  private BufferedImage[] bufferedImages;
  Transcriber(BufferedImage[] bufferedImages) {
    this.bufferedImages = bufferedImages;
  }

  public CompletableFuture<String> transcribeImages() {
    return null;
  }
}
