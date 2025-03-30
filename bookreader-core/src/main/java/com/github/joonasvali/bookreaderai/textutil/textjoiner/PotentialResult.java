package com.github.joonasvali.bookreaderai.textutil.textjoiner;

import com.github.joonasvali.bookreaderai.textutil.WordCount;
import com.github.joonasvali.bookreaderai.textutil.util.OffsetPenalty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PotentialResult {
  private final String[] sentences;
  private final String commonSentence;
  private final float evaluatedScore;
  private final int firstTextSentenceOffset;
  private final int secondTextSentenceOffset;
  private final int discardedWords;
  private final String firstSentence;
  private final String secondSentence;
  private final OffsetPenalty offsetPenaltyHelper;
  private final String[] firstSacrificedSentences;
  private final String[] secondSacrificedSentences;
  private final int commonSentenceIndex;
  private final List<SecondaryMatch> secondaryMatchCandidates;
  private final int firstSentencesCount;
  private final int secondSentencesCount;

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
        .map(secondaryMatch -> secondaryMatch.getScore())
        .reduce(0f, Float::sum) / Math.max(secondaryMatchCandidates.size(), 1);

    float maxOffsetPenaltyReduction = (float) commonSentenceIndex / Math.max(1, firstSentencesCount);


    float offsetFactor = offsetPenaltyHelper.calculateOffsetPenalty(firstTextSentenceOffset, secondTextSentenceOffset, (float) firstTextSentenceOffset / firstSentencesCount, (float) secondTextSentenceOffset / secondSentencesCount);
    float bonusFromTwoIncompleteSentencesFormingOne = getFormulateFullSentenceBonus(commonSentence, firstSentence, secondSentence);
    float penalty1 = evaluatedScore * offsetFactor * (1 - averageSecondaryMatchScore * maxOffsetPenaltyReduction);
    float penalty2 = evaluatedScore * discardedWords / WordCount.countWords(commonSentence);
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
    int totalWordsInSentences = Arrays.stream(sentences).mapToInt(WordCount::countWords).sum();
    for (SecondaryMatch match : secondaryMatchCandidates) {
      int words = WordCount.countWords(match.getSacrificedSentence());
      float weight = (float) words / totalWordsInSentences;
      totalPenalty += (1 - match.getScore()) * weight;
    }
    return Math.min(1.0f, totalPenalty);
  }

  public String[] getSentences() {
    return sentences;
  }

  public String getCommonSentence() {
    return commonSentence;
  }

  public float getEvaluatedScore() {
    return evaluatedScore;
  }

  public int getFirstTextSentenceOffset() {
    return firstTextSentenceOffset;
  }

  public int getSecondTextSentenceOffset() {
    return secondTextSentenceOffset;
  }

  public int getDiscardedWords() {
    return discardedWords;
  }

  public String getFirstSentence() {
    return firstSentence;
  }

  public String getSecondSentence() {
    return secondSentence;
  }

  public OffsetPenalty getOffsetPenaltyHelper() {
    return offsetPenaltyHelper;
  }

  public String[] getFirstSacrificedSentences() {
    return firstSacrificedSentences;
  }

  public String[] getSecondSacrificedSentences() {
    return secondSacrificedSentences;
  }

  public int getCommonSentenceIndex() {
    return commonSentenceIndex;
  }

  public List<SecondaryMatch> getSecondaryMatchCandidates() {
    return secondaryMatchCandidates;
  }

  public int getFirstSentencesCount() {
    return firstSentencesCount;
  }

  public int getSecondSentencesCount() {
    return secondSentencesCount;
  }

  public String getSentence(int index) {
    return sentences[index];
  }
}
