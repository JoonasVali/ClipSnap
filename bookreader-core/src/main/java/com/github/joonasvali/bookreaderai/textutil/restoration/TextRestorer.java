package com.github.joonasvali.bookreaderai.textutil.restoration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextRestorer {
  private Logger logger = LoggerFactory.getLogger(TextRestorer.class);
  public String restoreText(String ... texts) {
    TextAligner textAligner = new TextAligner();
    StringBuilder finalVersion = new StringBuilder();

    TextAligner.AlignmentResult result = textAligner.alignTexts(texts);

    char lastChar = !finalVersion.isEmpty() ? finalVersion.charAt(finalVersion.length() - 1) : '\n';
    if (lastChar != ' ' && lastChar != '\n') {
      finalVersion.append(" ");
    }
    finalVersion.append(result.getAlignedText());

    return finalVersion.toString();
  }
}
