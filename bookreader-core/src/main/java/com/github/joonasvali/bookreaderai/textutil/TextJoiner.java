package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.textutil.restoration.Sentence;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextSentenceMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextJoiner {

  // Use the fuzzy matcher for sentence comparison.
  private final SentencePotentialMatcher fuzzyMatcher = new SentencePotentialMatcher();

  public String join(String text1, String text2) {
    String t1 = text1.stripTrailing();
    String t2 = text2.stripTrailing();

    Sentence[] sentences1 = new TextSentenceMatcher().getSentences(t1);
    Sentence[] sentences2 = new TextSentenceMatcher().getSentences(t2);

    String[] result = join(
        Arrays.stream(sentences1)
            .map(Sentence::texts)
            .flatMap(Arrays::stream)
            .toArray(String[]::new),
        Arrays.stream(sentences2)
            .map(Sentence::texts)
            .flatMap(Arrays::stream)
            .toArray(String[]::new)
    );

    if (result == null) {
      result = new String[] { t1 + " " + t2 };
    }

    return sentencesToString(result);
  }

  private String sentencesToString(String[] sentences) {
    return Arrays.stream(sentences)
        .reduce((s1, s2) -> {
          if (!s1.endsWith("\n") && !s2.startsWith("\n")) {
            return s1 + " " + s2;
          }
          return s1 + s2;
        })
        .orElse("");
  }

  private String[] join(String[] sentences1, String[] sentences2) {
    System.out.println("Sentences1: " + Arrays.toString(sentences1));
    System.out.println("Sentences2: " + Arrays.toString(sentences2));


    List<PotentialResult> potentialResultList = new ArrayList<>();
    for (int firstSentenceIndex = sentences1.length - 1; firstSentenceIndex >= 0; firstSentenceIndex--) {
      for (int secondSentenceIndex = 0; secondSentenceIndex < sentences2.length; secondSentenceIndex++) {
        String firstSentence = sentences1[firstSentenceIndex];
        String secondSentence = sentences2[secondSentenceIndex];
        // This works in case there's just one sentence in each text.
        SentencePotentialMatcher.MatchResult result = fuzzyMatcher.match(firstSentence, secondSentence);

        int firstOffset = sentences1.length - 1 - firstSentenceIndex;
        int secondOffset = secondSentenceIndex;

        if (firstSentence.startsWith(result.prefix) && secondSentence.endsWith(result.suffix) && (result.score > 0.2 || (firstOffset == 0 && secondOffset == 0)) ) {
          String newSentence = result.prefix + result.commonPart + result.suffix;
          Set<String> candidateCommonSentence = new HashSet<>();
          candidateCommonSentence.add(newSentence);

          if (removePunctuationAndWhiteSpace(result.prefix).isEmpty() && removePunctuationAndWhiteSpace(result.suffix).isEmpty()) {
            candidateCommonSentence.add(firstSentence);
            candidateCommonSentence.add(secondSentence);
          }

          for (String sentence : candidateCommonSentence) {
            String[] candidate = buildSentences(sentences1, sentences2, firstSentenceIndex, secondSentenceIndex, sentence);
            PotentialResult potentialResult = new PotentialResult(candidate, sentence, result.score, firstOffset, secondOffset);
            potentialResultList.add(potentialResult);
          }
        }
      }
    }
    if (potentialResultList.isEmpty()) {
      return null;
    }

    // Get the one with max score:
    return potentialResultList
        .stream()

        .max((r1, r2) -> Float.compare(r1.getCalculatedScore(), r2.getCalculatedScore()))
        .map(r -> r.sentences)
        .orElse(null);

  }

  private String removePunctuationAndWhiteSpace(String prefix) {
    return prefix.replaceAll("[^a-zA-Z0-9]", "");
  }

  private String[] buildSentences(String[] sentences1, String[] sentences2, int firstSentenceIndex, int secondSentenceIndex, String commonSentence) {
    String[] returnedSentences = new String[firstSentenceIndex + sentences2.length - secondSentenceIndex];


    for (int i = 0; i < firstSentenceIndex; i++) {
      returnedSentences[i] = sentences1[i];
    }

    returnedSentences[firstSentenceIndex] = commonSentence;

    for (int i = 0; i < sentences2.length - (secondSentenceIndex + 1); i++) {
      returnedSentences[firstSentenceIndex + 1 + i] = sentences2[secondSentenceIndex + i + 1];
    }

    return returnedSentences;
  }

  private class PotentialResult {
    private String[] sentences;
    private String commonSentence;
    private float score;
    private int firstTextSentenceOffset;
    private int secondTextSentenceOffset;

    public PotentialResult(String[] sentences, String commonSentence, float score, int firstTextSentenceOffset, int secondTextSentenceOffset) {
      this.sentences = sentences;
      this.commonSentence = commonSentence;
      this.score = score;

      this.firstTextSentenceOffset = firstTextSentenceOffset;
      this.secondTextSentenceOffset = secondTextSentenceOffset;
    }

    public float getCalculatedScore() {
      if (firstTextSentenceOffset == 0 && secondTextSentenceOffset == 0) {
        return Math.max(score, 0.2f);
      }
      return Math.max(0.01f, score - 0.3f * (firstTextSentenceOffset + secondTextSentenceOffset));
    }
  }
}


