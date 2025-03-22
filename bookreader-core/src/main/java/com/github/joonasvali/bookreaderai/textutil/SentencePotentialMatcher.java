package com.github.joonasvali.bookreaderai.textutil;

import java.util.ArrayList;
import java.util.List;

public class SentencePotentialMatcher {

  /**
   * Computes a fuzzy match between s1 and s2 and returns a MatchResult.
   * The MatchResult contains:
   * - score: a similarity score (0 to 1)
   * - commonPart: the fuzzy common segment between the two (from the original text)
   * - prefix: the part of the container (original) that comes before the common part
   * - suffix: the part of the container (original) that comes after the common part
   *
   * In the fuzzy–containment case, the shorter string is taken as the commonPart,
   * and the container (the longer string) yields the prefix (before) or suffix (after).
   */
  public MatchResult match(String s1, String s2) {
    if (s1 == null || s2 == null) {
      return new MatchResult(0.0, "", "", "");
    }

    // Build normalized versions with mapping.
    NormalizedString ns1 = normalizeWithMapping(s1);
    NormalizedString ns2 = normalizeWithMapping(s2);

    if (ns1.normalized.isEmpty() || ns2.normalized.isEmpty()) {
      return new MatchResult(0.0, "", "", "");
    }

    // If the normalized strings are exactly equal, return full match.
    if (ns1.normalized.equals(ns2.normalized)) {
      return new MatchResult(1.0, s1, "", "");
    }

    // First, try fuzzy containment: does one normalized string (almost) appear inside the other?
    FuzzyContainmentResult fcr = null;
    boolean s1IsContained = false;
    if (ns1.normalized.length() <= ns2.normalized.length()) {
      fcr = fuzzyContainment(ns1.normalized, ns2.normalized);
      s1IsContained = (fcr != null);
    }
    if (fcr == null && ns2.normalized.length() < ns1.normalized.length()) {
      fcr = fuzzyContainment(ns2.normalized, ns1.normalized);
      s1IsContained = false; // in this case, s2 is the contained one.
    }

    double score;
    String commonPart;
    String prefix;
    String suffix;

    if (fcr != null) {
      score = fcr.similarity;
      if (s1IsContained) {
        // ns1 (shorter) is (fuzzily) contained in ns2.
        // Use the entire original s1 as the common part.
        commonPart = s1;
        prefix = "";
        suffix = s2.substring(s2.length());
      } else {
        // ns2 (shorter) is (fuzzily) contained in ns1.
        commonPart = s2;
        int origStart = ns1.mapping.get(fcr.startIndex);
        int adjustedOrigStart = origStart;
        int lastNewline = s1.lastIndexOf("\n", origStart);
        if (lastNewline != -1 && (origStart - lastNewline) <= 10) {
          adjustedOrigStart = lastNewline + 1;
        }
        prefix = s1.substring(0, adjustedOrigStart);
        suffix = "";
      }
    } else {
      // Fallback: use exact longest common substring (LCS) on the normalized strings.
      LCSResult lcsRes = getLongestCommonSubstring(ns1.normalized, ns2.normalized);
      score = Math.max(0.0, (double) lcsRes.length / Math.max(ns1.normalized.length(), ns2.normalized.length()));
      if (lcsRes.length == 0) {
        return new MatchResult(score, "", "", "");
      }
      int s1CommonStart = ns1.mapping.get(lcsRes.posA - lcsRes.length);
      int s1CommonEnd = ns1.mapping.get(lcsRes.posA - 1) + 1; // exclusive end
      int s2CommonStart = ns2.mapping.get(lcsRes.posB - lcsRes.length);
      int s2CommonEnd = ns2.mapping.get(lcsRes.posB - 1) + 1;
      commonPart = s1.substring(s1CommonStart, s1CommonEnd);
      prefix = s1.substring(0, s1CommonStart);
      suffix = s2.substring(s2CommonEnd);
    }

    return new MatchResult(score, commonPart, prefix, suffix);
  }

  /**
   * Tries to determine if 'shortStr' is (approximately) contained within 'longStr'
   * by sliding a window of length shortStr.length() over longStr.
   * Returns a FuzzyContainmentResult if the best similarity is above a threshold; otherwise, returns null.
   */
  private FuzzyContainmentResult fuzzyContainment(String shortStr, String longStr) {
    double bestSim = 0.0;
    int bestIndex = -1;
    int windowLen = shortStr.length();
    for (int i = 0; i <= longStr.length() - windowLen; i++) {
      String window = longStr.substring(i, i + windowLen);
      double sim = levenshteinSimilarity(shortStr, window);
      if (sim > bestSim) {
        bestSim = sim;
        bestIndex = i;
      }
      if (bestSim >= 0.9) {
        break;
      }
    }
    return bestSim >= 0.9 ? new FuzzyContainmentResult(bestSim, bestIndex) : null;
  }

  /**
   * Returns the longest common substring between a and b (exact match) and the ending positions.
   */
  private LCSResult getLongestCommonSubstring(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];
    int maxLen = 0;
    int posA = 0;
    int posB = 0;

    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        if (a.charAt(i - 1) == b.charAt(j - 1)) {
          dp[i][j] = dp[i - 1][j - 1] + 1;
          if (dp[i][j] > maxLen) {
            maxLen = dp[i][j];
            posA = i;
            posB = j;
          }
        }
      }
    }
    String substring = maxLen > 0 ? a.substring(posA - maxLen, posA) : "";
    return new LCSResult(substring, posA, posB, maxLen);
  }

  /**
   * Computes the Levenshtein similarity between two strings.
   * Returns a value between 0 and 1, where 1 means identical.
   */
  private double levenshteinSimilarity(String a, String b) {
    int distance = levenshteinDistance(a, b);
    int maxLen = Math.max(a.length(), b.length());
    return maxLen == 0 ? 1.0 : 1.0 - ((double) distance / maxLen);
  }

  /**
   * Computes the Levenshtein distance between two strings.
   */
  private int levenshteinDistance(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];
    for (int i = 0; i <= a.length(); i++) {
      dp[i][0] = i;
    }
    for (int j = 0; j <= b.length(); j++) {
      dp[0][j] = j;
    }
    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        dp[i][j] = Math.min(
            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
            dp[i - 1][j - 1] + cost);
      }
    }
    return dp[a.length()][b.length()];
  }

  /**
   * Converts the original string into a normalized string (lowercased, without punctuation)
   * while recording a mapping from each normalized character index to the original string index.
   * NOTE: Whitespace (including newlines) is preserved.
   */
  private NormalizedString normalizeWithMapping(String s) {
    StringBuilder normBuilder = new StringBuilder();
    List<Integer> mapList = new ArrayList<>();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (!isPunctuation(c)) {  // preserve letters, digits, and whitespace
        normBuilder.append(Character.toLowerCase(c));
        mapList.add(i);
      }
    }
    return new NormalizedString(s, normBuilder.toString(), mapList);
  }

  private boolean isPunctuation(char c) {
    // Consider any character that is not a letter, digit, or whitespace to be punctuation.
    return !Character.isLetterOrDigit(c) && !Character.isWhitespace(c);
  }

  /**
   * Checks if a string consists solely of punctuation and/or whitespace.
   */
  private boolean allPunctuationOrWhitespace(String s) {
    for (char c : s.toCharArray()) {
      if (!isPunctuation(c) && !Character.isWhitespace(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Encapsulates a normalized version of a string along with a mapping from each character in
   * the normalized string to its index in the original string.
   */
  private static class NormalizedString {
    final String original;
    final String normalized;
    final List<Integer> mapping; // mapping from normalized index to original index

    NormalizedString(String original, String normalized, List<Integer> mapping) {
      this.original = original;
      this.normalized = normalized;
      this.mapping = mapping;
    }
  }

  /**
   * Result type returned by match().
   */
  public static class MatchResult {
    public final double score;
    public final String commonPart;
    public final String prefix; // the part before the common part
    public final String suffix; // the part after the common part

    public MatchResult(double score, String commonPart, String prefix, String suffix) {
      this.score = score;
      this.commonPart = commonPart;
      this.prefix = prefix;
      this.suffix = suffix;
    }

    @Override
    public String toString() {
      return "MatchResult{" +
          "score=" + score +
          ", commonPart='" + commonPart + '\'' +
          ", prefix='" + prefix + '\'' +
          ", suffix='" + suffix + '\'' +
          '}';
    }
  }

  /**
   * Helper type for fuzzy containment results.
   */
  private static class FuzzyContainmentResult {
    final double similarity;
    final int startIndex;  // starting index in the container's normalized string

    FuzzyContainmentResult(double similarity, int startIndex) {
      this.similarity = similarity;
      this.startIndex = startIndex;
    }
  }

  /**
   * Helper type for longest common substring results.
   */
  private static class LCSResult {
    final String substring;
    final int posA;  // exclusive ending index in first normalized string
    final int posB;  // exclusive ending index in second normalized string
    final int length;

    LCSResult(String substring, int posA, int posB, int length) {
      this.substring = substring;
      this.posA = posA;
      this.posB = posB;
      this.length = length;
    }
  }
}
