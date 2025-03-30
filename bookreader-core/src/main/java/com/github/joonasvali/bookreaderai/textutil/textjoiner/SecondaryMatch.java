package com.github.joonasvali.bookreaderai.textutil.textjoiner;

import com.github.joonasvali.bookreaderai.textutil.SentencePotentialMatcher;

public class SecondaryMatch {
  private final String sacrificedSentence;
  private final String realSentence;
  private final float score;


  public SecondaryMatch(String sacrificedSentence, String realSentence) {
    this.sacrificedSentence = sacrificedSentence;
    this.realSentence = realSentence;
    SentencePotentialMatcher.MatchResult result = new SentencePotentialMatcher().match(sacrificedSentence, realSentence);
    this.score = result.score;
  }

  public String getSacrificedSentence() {
    return sacrificedSentence;
  }

  public String getRealSentence() {
    return realSentence;
  }

  public float getScore() {
    return score;
  }
}
