package com.github.joonasvali.bookreaderai.textutil.restoration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextRestorer {
  private Logger logger = LoggerFactory.getLogger(TextRestorer.class);
  public String restoreText(String ... texts) {
    TextSentenceMatcher textSentenceMatcher = new TextSentenceMatcher();
    Sentence[] sentences = textSentenceMatcher.getSentences(texts);

    // Check that there is equal amount of sentences in all texts
    int sentenceCount = sentences[0].texts().length;
    for (Sentence sentence : sentences) {
      if (sentence.texts().length != sentenceCount) {
        throw new IllegalArgumentException("Texts do not have equal amount of sentences");
      }
    }

    TextAligner textAligner = new TextAligner();
    StringBuilder finalVersion = new StringBuilder();

    for (int i = 0; i < sentences.length; i++) {
      Sentence sentenceVersions = sentences[i];
      TextAligner.AlignmentResult result = textAligner.alignTexts(sentenceVersions.texts());
      if (!result.isSuccess()) {
        logger.warn("Failed to align texts for sentence {}", i);
        return texts[0];
      }

      char lastChar = !finalVersion.isEmpty() ? finalVersion.charAt(finalVersion.length() - 1) : '\n';
      if (lastChar != ' ' && lastChar != '\n') {
        finalVersion.append(" ");
      }
      finalVersion.append(result.getAlignedText());
    }

    return finalVersion.toString();
  }
}
