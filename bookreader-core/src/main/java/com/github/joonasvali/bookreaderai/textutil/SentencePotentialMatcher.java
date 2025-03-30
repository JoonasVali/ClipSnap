package com.github.joonasvali.bookreaderai.textutil;

import java.util.ArrayList;
import java.util.List;

public class SentencePotentialMatcher {

  /**

   Primary entry point for matching s1 and s2. Tries multiple normalizations for
   each string to handle edge cases like hyphens differently, then picks the best
   scoring result. */
  public MatchResult match(String s1, String s2) {
    if (s1 == null || s2 == null) {
      return new MatchResult(0.0f, "", "", "");
    }
// Build multiple normalizations for each string, e.g. hyphen-as-space vs. hyphen-as-letter.
    List<NormalizedString> s1Candidates = buildAlternativeNormalizations(s1);
    List<NormalizedString> s2Candidates = buildAlternativeNormalizations(s2);

    float bestScore = 0.0f;
    PartialResult bestPartial = null;

// Try all combinations of normalizations between s1 and s2.
    for (NormalizedString ns1 : s1Candidates) {
      for (NormalizedString ns2 : s2Candidates) {
        PartialResult pr = matchNormalized(ns1, ns2);
        if (pr.score > bestScore) {
          bestScore = pr.score;
          bestPartial = pr;
        }
      }
    }

    if (bestPartial == null) {
      // Nothing matched well; return 0.
      return new MatchResult(0.0f, "", "", "");
    }

// Reconstruct prefix, suffix, and commonPart from whichever normalization combo worked best.
    return bestPartial.buildFinalMatchResult();
  }

  /**

   Return multiple possible normalizations, varying how hyphens get treated. You can
   add more “approaches” (for example, punctuation handling) if desired. */
  private List<NormalizedString> buildAlternativeNormalizations(String original) {
    List<NormalizedString> results = new ArrayList<>();
    results.add(normalizeWithMapping(original, HyphenMode.AS_SPACE));
    results.add(normalizeWithMapping(original, HyphenMode.AS_LETTER));
    return results;
  }

  /**

   The actual matching logic, taking pre-built NormalizedStrings for s1 and s2.
   This is essentially the original fuzzyContainment + LCS approach, but we return
   an intermediate “PartialResult” so we can figure out later how to reconstruct
   the prefix/suffix/commonPart from whichever approach yields the best score. */
  private PartialResult matchNormalized(NormalizedString ns1, NormalizedString ns2) { // If either is empty, no match.
    if (ns1.normalized.isEmpty() || ns2.normalized.isEmpty()) { return new PartialResult(0.0f, "", ns1, ns2, false, -1, false); }
    // If they match exactly, return 1.0
    if (ns1.normalized.equals(ns2.normalized)) {
      return new PartialResult(
          1.0f,
          ns1.original, // entire text as common part
          ns1,
          ns2,
          false,
          -1,
          true
      );
    }

// Decide if fuzzyContainment is allowed by length.
    boolean canFuzzyContain = Math.min(ns1.normalized.length(), ns2.normalized.length()) >= 3;

    FuzzyContainmentResult fcr = null;
    boolean s1IsContained = false;

    if (canFuzzyContain && ns1.normalized.length() <= ns2.normalized.length()) {
      fcr = fuzzyContainment(ns1.normalized, ns2.normalized);
      s1IsContained = (fcr != null);
    }
    if (fcr == null && canFuzzyContain && ns2.normalized.length() < ns1.normalized.length()) {
      fcr = fuzzyContainment(ns2.normalized, ns1.normalized);
      s1IsContained = false;
    }

    if (fcr != null) {
      // Found a fuzzy containment with a similarity above the threshold (≥0.9).
      return new PartialResult(fcr.similarity, "", ns1, ns2, s1IsContained, fcr.startIndex, false);
    } else {
      // Fall back to LCS approach
      LCSResult lcs = getLongestCommonSubstring(ns1.normalized, ns2.normalized);
      float lcsScore = (float) lcs.length / Math.max(ns1.normalized.length(), ns2.normalized.length());
      return new PartialResult(lcsScore, lcs.substring, ns1, ns2, false, -1, false, lcs);
    }
  }

  /**

   Helper data structure to hold intermediate match info before reconstructing
   the final prefix/suffix/commonPart. */
  private static class PartialResult {
    final float score;
    final String directSubstring; // used only in LCS fallback
    final NormalizedString ns1;
    final NormalizedString ns2;
    final boolean s1IsContained; // if we found fuzzy containment, which side is contained
    final int containmentStartIndex; // if fuzzy containment
    final boolean isExactMatch;
    final LCSResult lcsResult; // if used LCS

    // For fuzzy containment
    PartialResult(float score, String directSubstring,
                  NormalizedString ns1, NormalizedString ns2,
                  boolean s1IsContained, int containmentStartIndex,
                  boolean isExactMatch) {
      this(score, directSubstring, ns1, ns2, s1IsContained, containmentStartIndex, isExactMatch, null);
    }

    // For LCS
    PartialResult(float score, String directSubstring,
                  NormalizedString ns1, NormalizedString ns2,
                  boolean s1IsContained, int containmentStartIndex,
                  boolean isExactMatch,
                  LCSResult lcsResult) {
      this.score = score;
      this.directSubstring = directSubstring;
      this.ns1 = ns1;
      this.ns2 = ns2;
      this.s1IsContained = s1IsContained;
      this.containmentStartIndex = containmentStartIndex;
      this.isExactMatch = isExactMatch;
      this.lcsResult = lcsResult;
    }

    /**
     * Build the final MatchResult (with prefix/suffix/commonPart) from the
     * fuzzy containment or LCS information stored here.
     */
    MatchResult buildFinalMatchResult() {
      if (score <= 0.0f) {
        return new MatchResult(0.0f, "", "", "");
      }

      // Exact match
      if (isExactMatch) {
        return new MatchResult(1.0f, ns1.original, "", "");
      }

      // Fuzzy containment?
      if (containmentStartIndex >= 0) {
        // We found a fuzzy containment with similarity >= 0.9
        // s1IsContained => ns1 is contained in ns2
        // else => ns2 is contained in ns1
        if (s1IsContained) {
          // Use the entire original s1 as common
          // prefix =, suffix= from s2
          // But we typically consider suffix to be empty in these tests
          return new MatchResult(score, ns1.original, "", "");
        } else {
          // Use entire s2 as common; the prefix is that portion of s1
          // up to the aligned mapping, suffix is empty
          int origStart = ns1.mapping.get(containmentStartIndex);
          // try to see if we want to adjust for newlines
          int adjustedOrigStart = origStart;
          int lastNewline = ns1.original.lastIndexOf("\n", origStart);
          if (lastNewline != -1 && (origStart - lastNewline) <= 10) {
            adjustedOrigStart = lastNewline + 1;
          }
          String prefix = ns1.original.substring(0, adjustedOrigStart);
          return new MatchResult(score, ns2.original, prefix, "");
        }
      }

      // LCS fallback
      if (lcsResult != null && lcsResult.length > 0) {
        float finalScore = Math.max(0.0f, score); // clamp to >= 0
        int s1CommonStart = ns1.mapping.get(lcsResult.posA - lcsResult.length);
        int s1CommonEnd = ns1.mapping.get(lcsResult.posA - 1) + 1;
        int s2CommonStart = ns2.mapping.get(lcsResult.posB - lcsResult.length);
        int s2CommonEnd = ns2.mapping.get(lcsResult.posB - 1) + 1;

        String commonPart = ns1.original.substring(s1CommonStart, s1CommonEnd);

        if (commonPart.trim().isEmpty()) {
          return new MatchResult(0.0f, "", "", "");
        }
        String prefix = ns1.original.substring(0, s1CommonStart);
        String suffix = ns2.original.substring(s2CommonEnd);
        return new MatchResult(finalScore, commonPart, prefix, suffix);
      }

      // Fallback: no match
      return new MatchResult(0.0f, "", "", "");
    }
  }

  /**

   Tries to determine if 'shortStr' is (approximately) contained within 'longStr'
   by sliding a window of length shortStr.length() over longStr. */
  private FuzzyContainmentResult fuzzyContainment(String shortStr, String longStr) {
    float bestSim = 0.0f;
    int bestIndex = -1;
    int windowLen = shortStr.length();
    for (int i = 0; i <= longStr.length() - windowLen; i++) {
      String window = longStr.substring(i, i + windowLen);
      double sim = levenshteinSimilarity(shortStr, window);
      if (sim > bestSim) {
        bestSim = (float) sim;
        bestIndex = i;
      }
      if (bestSim >= 0.9f) {
        break;
      }
    }
    return bestSim >= 0.9f ? new FuzzyContainmentResult(bestSim, bestIndex) : null;
  }

  /**

   Computes the Levenshtein similarity between two strings (0.0 to 1.0). */
  private double levenshteinSimilarity(String a, String b) {
    int distance = levenshteinDistance(a, b);
    int maxLen = Math.max(a.length(), b.length());
    return (maxLen == 0) ? 1.0 : 1.0 - ((double) distance / maxLen);
  }

  /**

   Standard Levenshtein distance. */
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
        int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
        dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
      }
    }
    return dp[a.length()][b.length()];
  }

  /**

   Exact longest common substring with dynamic programming. */
  private LCSResult getLongestCommonSubstring(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];
    int maxLen = 0, posA = 0, posB = 0;
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
    String substring = (maxLen > 0) ? a.substring(posA - maxLen, posA) : "";
    return new LCSResult(substring, posA, posB, maxLen);
  }

  /**

   Two ways to handle hyphens: treat them as spaces or as part of the alphanumeric block. */
  private enum HyphenMode {AS_SPACE, AS_LETTER}

  /**

   Normalizes the original string in one of two ways:
   Hyphens → spaces (with punctuation, newlines, etc. collapsed into single spaces)
   2. Hyphens → letters (so "se-erious" → "seerious") */
  private NormalizedString normalizeWithMapping(String s, HyphenMode mode) {
    StringBuilder normBuilder = new StringBuilder();
    List<Integer> mapList = new ArrayList<>();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);

      // If letter/digit
      if (Character.isLetterOrDigit(c)) {
        normBuilder.append(Character.toLowerCase(c));
        mapList.add(i);
      } else if (c == '-' && mode == HyphenMode.AS_LETTER) {
        // If user wants hyphen to be treated as a letter bridging, skip adding a space
        // (but don't skip the char entirely—some might prefer to just remove it).
        // Here we remove the hyphen so "se-erious" becomes "seerious".
        // If you want to insert something else, adjust.
        // We'll do nothing (just remove it).
      } else {
        // unify everything else to a single space if last char wasn't already space
        if (normBuilder.length() == 0 || normBuilder.charAt(normBuilder.length() - 1) != ' ') {
          normBuilder.append(' ');
          mapList.add(i);
        }
      }
    }

// Trim trailing space if any
    while (normBuilder.length() > 0 && normBuilder.charAt(normBuilder.length() - 1) == ' ') {
      normBuilder.deleteCharAt(normBuilder.length() - 1);
    }

    return new NormalizedString(s, normBuilder.toString(), mapList);
  }

  /**

   Encapsulates a normalized version of a string along with a mapping from
   each character in the normalized string to its original index in the raw string. */
  private static class NormalizedString {
    final String original;
    final String normalized;
    final List<Integer> mapping;

    NormalizedString(String original, String normalized, List<Integer> mapping) {
      this.original = original;
      this.normalized = normalized;
      this.mapping = mapping;
    }
  }

  /**

   Public result type returned by match(). */
  public static class MatchResult {
    public final float score;
    public final String commonPart;
    public final String prefix;
    public final String suffix;

    public MatchResult(float score, String commonPart, String prefix, String suffix) {
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

   Helper data class for fuzzy containment results. */
  private static class FuzzyContainmentResult {
    final float similarity;
    final int startIndex; // starting index in the container's normalized string
    FuzzyContainmentResult(float similarity, int startIndex) { this.similarity = similarity; this.startIndex = startIndex; } }

    /**

     Helper data class for longest common substring results. */
    private static class LCSResult {
      final String substring;
      final int posA; // exclusive end index in a
      final int posB; // exclusive end index in b
      final int length;

      LCSResult(String substring, int posA, int posB, int length) {
        this.substring = substring;
        this.posA = posA;
        this.posB = posB;
        this.length = length;
      }
    }
  }