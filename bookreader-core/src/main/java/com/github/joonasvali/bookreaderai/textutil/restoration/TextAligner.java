package com.github.joonasvali.bookreaderai.textutil.restoration;

import com.github.joonasvali.bookreaderai.textutil.SentencePotentialMatcher;
import com.github.joonasvali.bookreaderai.textutil.SentencePotentialMatcher.MatchResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TextAligner {

  public static class AlignmentResult {
    private final String[][] alignedTexts;
    private final boolean success;

    public AlignmentResult(String[][] alignedTexts, boolean success) {
      this.alignedTexts = alignedTexts;
      this.success = success;
    }

    public String[][] getAlignedTexts() {
      return alignedTexts;
    }

    public boolean isSuccess() {
      return success;
    }
  }

  // A threshold for considering two sentences as similar enough.
  private static final double MATCH_THRESHOLD = 0.30;

  public AlignmentResult alignTexts(String[] textVersions) {
    if (textVersions == null || textVersions.length == 0) {
// Return one empty row to satisfy the test expectation.
      return new AlignmentResult(new String[][]{{}}, false);
    }

// Split each text into sentences.
    TextSentenceSplitter splitter = new TextSentenceSplitter();
    String[][] sentencesPerText = new String[textVersions.length][];
    int[] sentenceCounts = new int[textVersions.length];

    for (int i = 0; i < textVersions.length; i++) {
      if (textVersions[i] == null) {
        sentencesPerText[i] = new String[0];
        sentenceCounts[i] = 0;
      } else {
        sentencesPerText[i] = splitter.getSentences(textVersions[i]);
        sentenceCounts[i] = sentencesPerText[i].length;
      }
    }

// If there's only one text, return its sentences as the aligned result.
    if (textVersions.length == 1) {
      return new AlignmentResult(new String[][]{sentencesPerText[0]}, true);
    }

    // Compute the mode of the sentence counts.
    Map<Integer, Integer> countFreq = new HashMap<>();
    for (int count : sentenceCounts) {
      countFreq.put(count, countFreq.getOrDefault(count, 0) + 1);
    }

    int modeCount = -1;
    int modeFreq = 0;
    for (Map.Entry<Integer, Integer> entry : countFreq.entrySet()) {
      if (entry.getValue() > modeFreq) {
        modeFreq = entry.getValue();
        modeCount = entry.getKey();
      }
    }

    // Pick as baseline the first text with the modeCount number of sentences.
    int baselineIndex = -1;
    for (int i = 0; i < sentenceCounts.length; i++) {
      if (sentenceCounts[i] == modeCount) {
        baselineIndex = i;
        break;
      }
    }
    // Fallback: if no text has the mode (rare), choose the one with max sentences.
    if (baselineIndex == -1) {
      baselineIndex = 0;
      for (int i = 1; i < sentencesPerText.length; i++) {
        if (sentencesPerText[i].length > sentencesPerText[baselineIndex].length) {
          baselineIndex = i;
        }
      }
    }

    String[] baselineSentences = sentencesPerText[baselineIndex];
    int consensusCount = baselineSentences.length;

// Prepare the final result array.
    String[][] alignedTexts = new String[textVersions.length][consensusCount];

// Fuzzy matcher
    SentencePotentialMatcher matcher = new SentencePotentialMatcher();

// For each text version, compute the best alignment to the baseline.
    for (int t = 0; t < sentencesPerText.length; t++) {
      String[] candidateSentences = sentencesPerText[t];
      int[] bestMapping = computeBestMapping(baselineSentences, candidateSentences, matcher);

      // Build the aligned sentence array with bestMapping.
      for (int j = 0; j < consensusCount; j++) {
        int mappedIndex = bestMapping[j];
        if (mappedIndex == -1) {
          alignedTexts[t][j] = "";
        } else {
          alignedTexts[t][j] = candidateSentences[mappedIndex];
        }
      }
    }

    return new AlignmentResult(alignedTexts, true);
  }

  private int[] computeBestMapping(
      String[] baseline, String[] candidate, SentencePotentialMatcher matcher) {
    Map<String, DPResult> memo = new HashMap<>();
    DPResult res = dp(0, 0, baseline, candidate, matcher, memo);
    return res.mapping;
  }

  private static class DPResult {
    final double totalScore;
    final int[] mapping;  // mapping[i] = index in candidate for baseline[i], or -1 if unmatched

    DPResult(double totalScore, int[] mapping) {
      this.totalScore = totalScore;
      this.mapping = mapping;
    }
  }

  /**

   dp(i, j) returns the best alignment from baseline[i..] to candidate[j..].
   We consider:
   match baseline[i] with candidate[j] (if fuzzy match is >= MATCH_THRESHOLD)
   2. skip candidate[j] (extra sentence in candidate)
   3. skip baseline[i] (missing sentence in candidate)
   We use totalScore to measure alignment quality, and store the best mapping. */
  private DPResult dp(int i, int j, String[] baseline, String[] candidate, SentencePotentialMatcher matcher, Map<String, DPResult> memo) {
    int n = baseline.length;
    int m = candidate.length;
    String key = i + "_" + j;
    if (memo.containsKey(key)) {
      return memo.get(key);
    }

// If done with baseline, all are unmatched
    if (i == n) {
      int[] map = new int[n];
      Arrays.fill(map, -1);
      DPResult result = new DPResult(0.0, map);
      memo.put(key, result);
      return result;
    }
// If no more candidate sentences, all leftover baseline are unmatched
    if (j == m) {
      int[] map = new int[n];
      Arrays.fill(map, -1);
      DPResult result = new DPResult(0.0, map);
      memo.put(key, result);
      return result;
    }

    double bestScore = Double.NEGATIVE_INFINITY;
    DPResult best = null;
    boolean usedMatch = false;

// 1) Match baseline[i] with candidate[j] if the fuzzy score >= MATCH_THRESHOLD
    MatchResult matchResult = matcher.match(baseline[i], candidate[j]);
    if (matchResult != null && matchResult.score >= MATCH_THRESHOLD) {
      DPResult sub = dp(i + 1, j + 1, baseline, candidate, matcher, memo);
      double score = matchResult.score + sub.totalScore;
      bestScore = score;
      int[] map = new int[n];
      System.arraycopy(sub.mapping, 0, map, 0, n);
      map[i] = j;
      best = new DPResult(score, map);
      usedMatch = true;
    }

// 2) Skip candidate[j] (candidate has an extra sentence)
    {
      DPResult sub = dp(i, j + 1, baseline, candidate, matcher, memo);
      double score = sub.totalScore;
      // Tie-break: prefer an actual match if scores are identical
      if (score > bestScore || (score == bestScore && !usedMatch)) {
        bestScore = score;
        int[] map = new int[n];
        System.arraycopy(sub.mapping, 0, map, 0, n);
        best = new DPResult(score, map);
        usedMatch = false;
      }
    }

// 3) Skip baseline[i] (missing sentence in candidate)
    {
      DPResult sub = dp(i + 1, j, baseline, candidate, matcher, memo);
      double score = sub.totalScore;
      if (score > bestScore || (score == bestScore && !usedMatch)) {
        bestScore = score;
        int[] map = new int[n];
        System.arraycopy(sub.mapping, 0, map, 0, n);
        map[i] = -1;
        best = new DPResult(score, map);
        usedMatch = false;
      }
    }

    memo.put(key, best);
    return best;
  }
}

