package com.github.joonasvali.bookreaderai.textutil.restoration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextRestorer {
  private Logger logger = LoggerFactory.getLogger(TextRestorer.class);

  public String restoreText(String... texts) {
    TextAligner textAligner = new TextAligner();
    MajorityVoter majorityVoter = new MajorityVoter();
    StringBuilder finalVersion = new StringBuilder();
    String[][] alignedTexts = textAligner.alignTexts(texts).getAlignedTexts();
    int maxLength = 0;
    for (String[] at : alignedTexts) {
      if (at.length > maxLength) {
        maxLength = at.length;
      }
    }
    for (int sentenceIndex = 0; sentenceIndex < maxLength; sentenceIndex++) {
      String[] candidates = new String[alignedTexts.length];
      for (int textIndex = 0; textIndex < alignedTexts.length; textIndex++) {
        candidates[textIndex] = sentenceIndex < alignedTexts[textIndex].length ? alignedTexts[textIndex][sentenceIndex] : "";
      }
      MajorityVoter.VoteResult voteResult = majorityVoter.vote(candidates);
      String chosen = voteResult.isSuccess() ? voteResult.getResultingText() : candidates[0];
      if (!finalVersion.isEmpty() && !chosen.isEmpty()) {
        if (finalVersion.charAt(finalVersion.length() - 1) != '\n' && finalVersion.charAt(finalVersion.length() - 1) != ' ') {
          finalVersion.append(" ");
        }
      }
      finalVersion.append(chosen);
    }
    return finalVersion.toString();
  }
}