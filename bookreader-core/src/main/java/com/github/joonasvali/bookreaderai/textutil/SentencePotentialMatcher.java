package com.github.joonasvali.bookreaderai.textutil;


public class SentencePotentialMatcher {

  /**
   * Returns a score between 0 and 1 indicating how closely the two sentences match.
   * A score of 1.0 represents a perfect match, while 0 indicates no match.
   */
  public double matchScore(String s1, String s2) {
    if (s1 == null || s2 == null) return 0.0;

    // Normalize both sentences
    String n1 = normalize(s1);
    String n2 = normalize(s2);

    // If either sentence is empty after normalization, return 0.
    if (n1.isEmpty() || n2.isEmpty()) {
      return 0.0;
    }

    // Check for direct equality or prefix matches.
    if (n1.equals(n2)) {
      return 1.0;
    }

    double score = 0.0;

    // If one sentence starts with the other, consider that a strong match.
    if (n1.startsWith(n2) || n2.startsWith(n1)) {
      score = 0.9;
    }

    // Additional fuzzy matching: if one sentence ends with an ellipsis,
    // remove it and re-check the score.
    if (s1.trim().endsWith("…")) {
      String s1NoEllipsis = s1.trim().substring(0, s1.trim().length() - 1);
      double ellipsisScore = matchScore(s1NoEllipsis, s2);
      score = Math.max(score, ellipsisScore);
    }
    if (s2.trim().endsWith("…")) {
      String s2NoEllipsis = s2.trim().substring(0, s2.trim().length() - 1);
      double ellipsisScore = matchScore(s1, s2NoEllipsis);
      score = Math.max(score, ellipsisScore);
    }

    // Fuzzy matching example: based on longest common substring ratio.
    double lcsRatio = longestCommonSubstringRatio(n1, n2);
    score = Math.max(score, lcsRatio);

    return score;
  }

  /**
   * A simple implementation to calculate the longest common substring ratio.
   * Returns a ratio between 0 and 1, where 1 means complete match.
   */
  private double longestCommonSubstringRatio(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];
    int longest = 0;

    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        if (a.charAt(i - 1) == b.charAt(j - 1)) {
          dp[i][j] = dp[i - 1][j - 1] + 1;
          longest = Math.max(longest, dp[i][j]);
        }
      }
    }

    // Normalize by the length of the longer string.
    int maxLen = Math.max(a.length(), b.length());
    return maxLen == 0 ? 0.0 : (double) longest / maxLen;
  }

  private String normalize(String s) {
    // Lowercase and remove all whitespace and punctuation.
    return s.toLowerCase().replaceAll("[\\s\\p{Punct}]+", "");
  }
}