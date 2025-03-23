package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.textutil.restoration.Sentence;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextSentenceMatcher;
import com.github.joonasvali.bookreaderai.textutil.util.OffsetPenalty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextJoiner {
  // Use the fuzzy matcher for sentence comparison.
  private final SentencePotentialMatcher fuzzyMatcher = new SentencePotentialMatcher();

  public String join(String text1, String text2) {

    Sentence[] sentences1 = new TextSentenceMatcher().getSentences(text1);
    Sentence[] sentences2 = new TextSentenceMatcher().getSentences(text2);

    PotentialResult[] potentialResults = join(
        Arrays.stream(sentences1)
            .map(Sentence::texts)
            .flatMap(Arrays::stream)
            .toArray(String[]::new),
        Arrays.stream(sentences2)
            .map(Sentence::texts)
            .flatMap(Arrays::stream)
            .toArray(String[]::new)
    );

    String[] result = potentialResults != null ? potentialResults[0].sentences : null;

    if (result == null) {
      result = new String[] { text1 + " " + text2 };
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

  private PotentialResult[] join(String[] sentences1, String[] sentences2) {
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
            int discardedWordsFirst = countDiscardedWords(firstSentence, sentence);
            int discardedWordsSecond = countDiscardedWords(secondSentence, sentence);

            String[] candidate = buildSentences(sentences1, sentences2, firstSentenceIndex, secondSentenceIndex, sentence);
            PotentialResult potentialResult = new PotentialResult(candidate, sentence, result.score, firstOffset, secondOffset, Math.max(discardedWordsFirst, discardedWordsSecond), firstSentence, secondSentence);
            potentialResultList.add(potentialResult);
          }
        }
      }
    }

    boolean hasOnlyTouchingSentenceWithNoMatch =
        potentialResultList.size() == 1 &&
        // 50%+ of words are discarded as a result of the join
        potentialResultList.getFirst().discardedWords >= (countWords(potentialResultList.getFirst().commonSentence) + potentialResultList.getFirst().discardedWords) * 0.5f &&
        potentialResultList.getFirst().firstTextSentenceOffset == 0 &&
        potentialResultList.getFirst().secondTextSentenceOffset == 0 &&
        potentialResultList.getFirst().evaluatedScore < 0.2f;

    if (potentialResultList.isEmpty() || hasOnlyTouchingSentenceWithNoMatch) {
      return null;
    }

    double maxScore = potentialResultList
        .stream()
        .mapToDouble(PotentialResult::getCalculatedScore).max().orElse(0);

    // Get the ones with max score:
    PotentialResult[] maxScored = potentialResultList
        .stream()
        .filter(r -> r.getCalculatedScore() >= maxScore - 0.00001)
        .toArray(PotentialResult[]::new);

    return maxScored;
  }

  private int countWords(String commonSentence) {
    if (commonSentence == null) {
      return 0;
    }
    return commonSentence.replaceAll("[\\p{Punct}]", "").split("\\s+").length;
  }

  private int countDiscardedWords(String sentence, String match) {
    if (sentence == null || match == null) {
      return 0;
    }
    // Remove punctuation and convert to lowercase for both the sentence and the match string.
    String cleanedSentence = sentence.replaceAll("[\\p{Punct}]", "").toLowerCase();
    String cleanedMatch = match.replaceAll("[\\p{Punct}]", "").toLowerCase();

    // Split the cleaned sentence by whitespace.
    String[] words = cleanedSentence.split("\\s+");
    String[] matchingWords = cleanedMatch.split("\\s+");

    return Math.max(0, words.length - matchingWords.length);
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
    private float evaluatedScore;
    private int firstTextSentenceOffset;
    private int secondTextSentenceOffset;
    private int discardedWords;
    private String firstSentence;
    private String secondSentence;
    private OffsetPenalty offsetPenaltyHelper;

    public PotentialResult(String[] sentences, String commonSentence, float evaluatedScore, int firstTextSentenceOffset, int secondTextSentenceOffset, int discardedWords, String firstSentence, String secondSentence) {
      this.sentences = sentences;
      this.commonSentence = commonSentence;
      this.evaluatedScore = evaluatedScore;

      this.firstTextSentenceOffset = firstTextSentenceOffset;
      this.secondTextSentenceOffset = secondTextSentenceOffset;
      this.discardedWords = discardedWords;
      this.firstSentence = firstSentence;
      this.secondSentence = secondSentence;
      // TODO offset penalty should be smaller if "previous" sentences are very small.
      this.offsetPenaltyHelper = new OffsetPenalty(0.65f);
    }

    public float getCalculatedScore() {
      float offsetFactor = offsetPenaltyHelper.calculateOffsetPenalty(firstTextSentenceOffset, secondTextSentenceOffset);
      float penalty1 = evaluatedScore * offsetFactor;
      float penalty2 = evaluatedScore * discardedWords / countWords(commonSentence);
      float calcScore = Math.max(0.01f, evaluatedScore - penalty1 - penalty2);

      if (firstTextSentenceOffset == 0 && secondTextSentenceOffset == 0) {
        calcScore = Math.max(calcScore, 0.1f);
      }
      return calcScore;
    }
  }
}


