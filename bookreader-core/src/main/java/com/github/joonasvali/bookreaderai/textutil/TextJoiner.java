package com.github.joonasvali.bookreaderai.textutil;

/**
 * A class that joins two content snippets together, trying to avoid repeating the same information.
 *
 * This class is 100% AI generated using test-driven development, good luck!
 */
public class TextJoiner {

  public String join(String text1, String text2) {
    if (text1 == null) text1 = "";
    if (text2 == null) text2 = "";

    // Remove extra leading/trailing whitespace.
    String s1 = text1.strip();
    String s2 = text2.strip();
    if (s1.isEmpty()) return s2;
    if (s2.isEmpty()) return s1;

    // Normalize s1 for overlap detection.
    NormalizedResult nr1 = normalize(s1);

    // Try to find the best overlap from s2.
    Overlap bestOverlap = new Overlap(0, 0, null);
    // Only consider candidates starting at word boundaries.
    for (int j = 0; j < s2.length(); j++) {
      if (j == 0 || Character.isWhitespace(s2.charAt(j - 1))) {
        String candidate = s2.substring(j);
        NormalizedResult nrCandidate = normalize(candidate);
        int common = getCommonOverlap(nr1.normalized, nrCandidate.normalized);
        if (common > bestOverlap.length) {
          bestOverlap.length = common;
          bestOverlap.startInS2 = j;
          bestOverlap.normCandidate = nrCandidate;
        }
      }
    }

    // Discard a one-character overlap if it doesn't start at index 0 to avoid false positives.
    if (bestOverlap.length == 1 && bestOverlap.startInS2 != 0) {
      bestOverlap.length = 0;
    }

    String joined;
    // If we found a nonzero overlap, remove the overlapped part from s2.
    if (bestOverlap.length > 0) {
      int offsetInCandidate = bestOverlap.normCandidate.getOriginalIndexForNormalizedIndex(bestOverlap.length);
      int joinIndexInS2 = bestOverlap.startInS2 + offsetInCandidate;
      joined = s1 + s2.substring(joinIndexInS2);
    } else {
      // No valid overlap found: join with a space if needed.
      if (s1.endsWith(" ") || s2.startsWith(" ")) {
        joined = s1 + s2;
      } else {
        joined = s1 + " " + s2;
      }
    }

    // Fix the ending punctuation if necessary.
    return fixEndingPunctuation(joined);
  }

  // Returns the length (in normalized characters) of the largest overlap where the suffix of norm1
  // equals the prefix of norm2.
  private int getCommonOverlap(String norm1, String norm2) {
    int max = Math.min(norm1.length(), norm2.length());
    int best = 0;
    for (int k = 1; k <= max; k++) {
      String suffix = norm1.substring(norm1.length() - k);
      String prefix = norm2.substring(0, k);
      if (suffix.equals(prefix)) {
        best = k;
      }
    }
    return best;
  }

  // Normalize a string by removing whitespace and punctuation and converting to lower case.
  // Only letters and digits are kept.
  private NormalizedResult normalize(String s) {
    StringBuilder sb = new StringBuilder();
    int[] mapping = new int[s.length()]; // worst-case size
    int count = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isLetterOrDigit(c)) {
        sb.append(Character.toLowerCase(c));
        mapping[count] = i;
        count++;
      }
    }
    int[] mappingTrunc = new int[count];
    System.arraycopy(mapping, 0, mappingTrunc, 0, count);
    return new NormalizedResult(sb.toString(), mappingTrunc);
  }

  // Helper method to fix ending punctuation:
  // If the last non-whitespace character is punctuation but not a period, question mark, or exclamation mark,
  // then replace it with a period.
  private String fixEndingPunctuation(String text) {
    int i = text.length() - 1;
    while (i >= 0 && Character.isWhitespace(text.charAt(i))) {
      i--;
    }
    if (i >= 0) {
      char last = text.charAt(i);
      // Check if it's punctuation (and not one of . ? !)
      if (!Character.isLetterOrDigit(last) && last != '.' && last != '?' && last != '!') {
        text = text.substring(0, i) + "." + text.substring(i + 1);
      }
    }
    return text;
  }

  // Helper class that holds a normalized version of a string along with a mapping from each
  // character in the normalized string back to its index in the original string.
  private static class NormalizedResult {
    String normalized;
    // mapping[i] gives the index in the original string of the i-th character in normalized.
    int[] mapping;

    NormalizedResult(String normalized, int[] mapping) {
      this.normalized = normalized;
      this.mapping = mapping;
    }

    // Given a normalized overlap length (number of normalized characters),
    // return the corresponding offset in the original string.
    int getOriginalIndexForNormalizedIndex(int normIndex) {
      if (normIndex == 0) return 0;
      return mapping[normIndex - 1] + 1;
    }
  }

  // Helper class to store details about an overlap candidate.
  private static class Overlap {
    int length;           // overlap length (in normalized characters)
    int startInS2;        // starting index in s2 (the original trimmed s2) for the candidate
    NormalizedResult normCandidate; // normalized result for the candidate substring

    Overlap(int length, int startInS2, NormalizedResult normCandidate) {
      this.length = length;
      this.startInS2 = startInS2;
      this.normCandidate = normCandidate;
    }
  }
}