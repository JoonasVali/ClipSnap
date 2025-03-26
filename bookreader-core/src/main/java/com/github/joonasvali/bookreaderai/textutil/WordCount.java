package com.github.joonasvali.bookreaderai.textutil;

public class WordCount {
  public static int countWords(String commonSentence) {
    if (commonSentence == null) {
      return 0;
    }
    return commonSentence.replaceAll("[\\p{Punct}]", "").split("\\s+").length;
  }
}
