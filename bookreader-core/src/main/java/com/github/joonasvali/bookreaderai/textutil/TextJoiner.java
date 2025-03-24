package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.textutil.restoration.TextSentenceSplitter;
import com.github.joonasvali.bookreaderai.textutil.util.OffsetPenalty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextJoiner {
  private final SentencePotentialMatcher fuzzyMatcher = new SentencePotentialMatcher();

  public String join(String text1, String text2) {

    String[] sentences1 = new TextSentenceSplitter().getSentences(text1);
    String[] sentences2 = new TextSentenceSplitter().getSentences(text2);

    PotentialResult[] potentialResults = join(
        sentences1,
        sentences2
    );

    String[] result = potentialResults != null ? potentialResults[0].sentences : null;

    if (result == null) {
      result = new String[] { text1, text2 };
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

          String[] sacrificedSentencesFromFirst = sliceSentences(sentences1, firstSentenceIndex, true);
          String[] sacrificedSentencedFromSecond = sliceSentences(sentences2, secondSentenceIndex, false);

          for (String sentence : candidateCommonSentence) {
            int discardedWordsFirst = countDiscardedWords(firstSentence, sentence);
            int discardedWordsSecond = countDiscardedWords(secondSentence, sentence);

            String[] candidate = buildSentences(sentences1, sentences2, firstSentenceIndex, secondSentenceIndex, sentence);
            PotentialResult potentialResult = new PotentialResult(
                candidate, sentences1.length, sentences2.length, sentence, result.score, Math.max(discardedWordsFirst, discardedWordsSecond), firstSentence, secondSentence,
                sacrificedSentencesFromFirst, sacrificedSentencedFromSecond, firstSentenceIndex
            );
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

    if (maxScore < 0.11) {
      // With so low score we have no confidence in the result. Let's just append.
      return null;
    }

    // Get the ones with max score:
    PotentialResult[] maxScored = potentialResultList
        .stream()
        .filter(r -> r.getCalculatedScore() >= maxScore - 0.00001)
        .toArray(PotentialResult[]::new);

    return maxScored;
  }

  public String[] sliceSentences(String[] sentences, int indexExcluding, boolean forward) {
    if (forward) {
      return Arrays.copyOfRange(sentences, indexExcluding + 1, sentences.length);
    } else {
      return Arrays.copyOfRange(sentences, 0, indexExcluding);
    }
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
    private String[] firstSacrificedSentences;
    private String[] secondSacrificedSentences;
    private int commonSentenceIndex;
    private List<SecondaryMatch> secondaryMatchCandidates;
    private int firstSentencesCount;
    private int secondSentencesCount;

    public PotentialResult(String[] sentences, int firstSentencesCount, int secondSentencesCount, String commonSentence, float evaluatedScore, int discardedWords, String firstSentence, String secondSentence, String[] firstSacrificedSentences, String[] secondSacrificedSentences, int commonSentenceIndex) {
      this.sentences = sentences;
      this.commonSentence = commonSentence;
      this.evaluatedScore = evaluatedScore;
      this.commonSentenceIndex = commonSentenceIndex;
      this.firstSentencesCount = firstSentencesCount;
      this.secondSentencesCount = secondSentencesCount;
      this.discardedWords = discardedWords;
      this.firstSentence = firstSentence;
      this.secondSentence = secondSentence;
      this.firstSacrificedSentences = firstSacrificedSentences;
      this.secondSacrificedSentences = secondSacrificedSentences;
      this.firstTextSentenceOffset = this.firstSacrificedSentences.length;
      this.secondTextSentenceOffset = this.secondSacrificedSentences.length;
      // TODO offset penalty should be smaller if "previous" sentences are very small.
      this.offsetPenaltyHelper = new OffsetPenalty();

      secondaryMatchCandidates = new ArrayList<>();
      for (int i = 0; i < secondSacrificedSentences.length; i++) {
        if (commonSentenceIndex - (i + 1) >= 0) {
          SecondaryMatch match = new SecondaryMatch(secondSacrificedSentences[i], sentences[commonSentenceIndex - (i + 1)]);
          secondaryMatchCandidates.add(match);
        }
      }

      for (int i = 0; i < firstSacrificedSentences.length; i++) {
        if (commonSentenceIndex + (i + 1) < sentences.length) {
          SecondaryMatch match = new SecondaryMatch(firstSacrificedSentences[i], sentences[commonSentenceIndex + (i + 1)]);
          secondaryMatchCandidates.add(match);
        }
      }
    }

    public float getCalculatedScore() {
      // If more sentences match at this position, then we reduce the offset penalty.
      float averageSecondaryMatchScore = secondaryMatchCandidates
          .stream()
          .map(secondaryMatch -> secondaryMatch.score)
          .reduce(0f, Float::sum) / Math.max(secondaryMatchCandidates.size(), 1);

      float maxOffsetPenaltyReduction = (float) commonSentenceIndex / Math.max(1, firstSentencesCount);


      float offsetFactor = offsetPenaltyHelper.calculateOffsetPenalty(firstTextSentenceOffset, secondTextSentenceOffset, (float) firstTextSentenceOffset / firstSentencesCount, (float) secondTextSentenceOffset / secondSentencesCount);
      float bonusFromTwoIncompleteSentencesFormingOne = getFormulateFullSentenceBonus(commonSentence, firstSentence, secondSentence);
      float penalty1 = evaluatedScore * offsetFactor * (1 - averageSecondaryMatchScore * maxOffsetPenaltyReduction);
      float penalty2 = evaluatedScore * discardedWords / countWords(commonSentence);
      float penalty3 = evaluatedScore * calculatePenaltyFromMismatchedWordsInSecondaryMatches(secondaryMatchCandidates);
      float calcScore = Math.min(1.0f, Math.max(0.01f, evaluatedScore - penalty1 - penalty2 - penalty3 + bonusFromTwoIncompleteSentencesFormingOne));

      if (firstTextSentenceOffset == 0 && secondTextSentenceOffset == 0) {
        calcScore = Math.max(calcScore, 0.1f);
      }
      return calcScore;
    }

    private float getFormulateFullSentenceBonus(String commonSentence, String firstSentence, String secondSentence) {
      return hasPunctuation(commonSentence) && !hasPunctuation(firstSentence) && hasPunctuation(secondSentence) ? 0.25f : 0;
    }

    private boolean hasPunctuation(String sentence) {
      // Replace three or more consecutive dots with space. This is to avoid matching ellipsis as punctuation.
      sentence = sentence.replaceAll("\\.{3,}", " ");
      return sentence.contains(".") || sentence.contains("!") || sentence.contains("?") || sentence.contains(";");
    }

    private float calculatePenaltyFromMismatchedWordsInSecondaryMatches(List<SecondaryMatch> secondaryMatchCandidates) {
      float totalPenalty = 0;
      int totalWordsInSentences = Arrays.stream(sentences).mapToInt(TextJoiner.this::countWords).sum();
      for (SecondaryMatch match : secondaryMatchCandidates) {
        int words = countWords(match.sacrificedSentence);
        float weight = (float) words / totalWordsInSentences;
        totalPenalty += (1 - match.score) * weight;
      }
      return Math.min(1.0f, totalPenalty);
    }
  }

  private static class SecondaryMatch {
    private final String sacrificedSentence;
    private final String realSentence;
    private final float score;


    public SecondaryMatch(String sacrificedSentence, String realSentence) {
      this.sacrificedSentence = sacrificedSentence;
      this.realSentence = realSentence;
      SentencePotentialMatcher.MatchResult result = new SentencePotentialMatcher().match(sacrificedSentence, realSentence);
      this.score = result.score;
    }
  }
}


