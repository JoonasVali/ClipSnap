package com.github.joonasvali.bookreaderai.textutil.restoration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MajorityVoter {

  public VoteResult vote(String[] texts) {
    if (texts == null || texts.length == 0) {
      return new VoteResult(false, "");
    }

    int numTexts = texts.length;
    // Majority threshold: ceil(numTexts/2). For 3 texts, voteThreshold becomes 2.
    int voteThreshold = (numTexts + 1) / 2;

    // Tokenize each text into alternating whitespace and non-whitespace groups.
    Pattern tokenPattern = Pattern.compile("\\s+|\\S+");
    List<List<String>> tokenLists = new ArrayList<>();

    for (String text : texts) {
      if (text == null) {
        text = "";
      }
      List<String> tokens = new ArrayList<>();
      Matcher matcher = tokenPattern.matcher(text);
      while (matcher.find()) {
        tokens.add(matcher.group());
      }
      // Force splitting any token that contains a hyphen (if longer than one character),
      // so that e.g. "Cats-and-dogs.\n" always becomes
      // ["Cats", " ", "and", " ", "dogs.", "\n"]
      tokens = refineHyphenTokens(tokens);
      tokenLists.add(tokens);
    }

    // Determine maximum token count across all texts.
    int maxTokens = 0;
    for (List<String> tokens : tokenLists) {
      if (tokens.size() > maxTokens) {
        maxTokens = tokens.size();
      }
    }

    // We'll build the voted token list column-by–column.
    List<String> votedTokens = new ArrayList<>();
    boolean failedDueToDisagreement = false;

    // Process each token position from 0 up to maxTokens.
    for (int i = 0; i < maxTokens; i++) {
      int providedCount = 0;
      Map<String, Integer> counts = new HashMap<>();
      // For each text, if it has a token in this column, count it.
      for (List<String> tokenList : tokenLists) {
        if (i < tokenList.size()) {
          providedCount++;
          String token = tokenList.get(i);
          counts.put(token, counts.getOrDefault(token, 0) + 1);
        }
      }

      if (providedCount >= voteThreshold) {
        // (1) Try an exact vote first.
        String bestCandidateExact = null;
        int bestExactCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
          if (entry.getValue() >= voteThreshold && entry.getValue() > bestExactCount) {
            bestExactCount = entry.getValue();
            bestCandidateExact = entry.getKey();
          }
        }
        if (bestCandidateExact != null) {
          votedTokens.add(bestCandidateExact);
          continue;  // move to next column
        }

        // (2) Fallback: try fuzzy matching. For each candidate, sum the counts of any token
        // in that column that is "similar" (by our isSimilar method) to it.
        String bestCandidateFuzzy = null;
        int bestEffectiveCount = 0;
        int bestRawCount = 0;
        for (String candidate : counts.keySet()) {
          int effectiveCount = 0;
          for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (isSimilar(candidate, entry.getKey())) {
              effectiveCount += entry.getValue();
            }
          }
          int rawCount = counts.get(candidate);
          if (effectiveCount > bestEffectiveCount || (effectiveCount == bestEffectiveCount && rawCount > bestRawCount)) {
            bestEffectiveCount = effectiveCount;
            bestCandidateFuzzy = candidate;
            bestRawCount = rawCount;
          }
        }
        if (bestEffectiveCount >= voteThreshold) {
          votedTokens.add(bestCandidateFuzzy);
        } else {
          // Here, enough texts contributed a token but no candidate reaches the majority.
          // That is considered a disagreement. In that case we mark the vote as a failure.
          failedDueToDisagreement = true;
          break;
        }
      } else {
        // If fewer than voteThreshold texts have tokens for this column,
        // we interpret that as an indication that we've reached the (shorter) end of at least one text.
        // In that case we simply stop voting (without triggering a failure).
        break;
      }
    }

    // If we encountered a disagreement in a column where a majority had provided tokens,
    // then the overall vote fails completely.
    if (failedDueToDisagreement) {
      return new VoteResult(false, "");
    }

    // Otherwise, assemble the voted tokens.
    StringBuilder resultBuilder = new StringBuilder();
    for (String token : votedTokens) {
      resultBuilder.append(token);
    }
    String votedText = resultBuilder.toString();
    boolean success = !votedText.isEmpty();
    return new VoteResult(success, votedText);
  }

  // refineHyphenTokens:
  // If a non-whitespace token contains a hyphen and its length is greater than one,
  // split it on '-' and interleave a single space between the parts.
  private List<String> refineHyphenTokens(List<String> tokens) {
    List<String> refined = new ArrayList<>();
    for (String token : tokens) {
      if (!token.trim().isEmpty() && token.contains("-") && token.length() > 1) {
        String[] parts = token.split("-");
        if (parts.length > 1) {
          refined.add(parts[0]);
          for (int i = 1; i < parts.length; i++) {
            refined.add(" ");
            refined.add(parts[i]);
          }
          continue;
        }
      }
      refined.add(token);
    }
    return refined;
  }

  // isSimilar returns true if either the tokens are exactly equal or if they differ by at most one edit.
  // The edit check works as follows:
  //  - If strings are equal, return true.
  //  - If their length difference is more than 1, return false.
  //  - If they are of the same length, allow at most one substitution.
  //  - If their lengths differ by one, allow one insertion/deletion.
  private boolean isSimilar(String s1, String s2) {
    if (s1.equals(s2)) {
      return true;
    }
    int len1 = s1.length();
    int len2 = s2.length();
    if (Math.abs(len1 - len2) > 1) {
      return false;
    }
    if (len1 == len2) { // Check for substitution.
      int diff = 0;
      for (int i = 0; i < len1; i++) {
        if (s1.charAt(i) != s2.charAt(i)) {
          diff++;
          if (diff > 1) {
            return false;
          }
        }
      }
      return true;
    }
    // Check for one insertion/deletion.
    String shorter = len1 < len2 ? s1 : s2;
    String longer = len1 < len2 ? s2 : s1;
    int i = 0, j = 0;
    int diff = 0;
    while (i < shorter.length() && j < longer.length()) {
      if (shorter.charAt(i) == longer.charAt(j)) {
        i++;
        j++;
      } else {
        diff++;
        j++;  // Skip one character from the longer string.
        if (diff > 1) {
          return false;
        }
      }
    }
    if (j < longer.length()) {
      diff++;
    }
    return diff <= 1;
  }

  public static class VoteResult {
    private final boolean success;
    private final String resultingText;

    public VoteResult(boolean success, String resultingText) {
      this.success = success;
      this.resultingText = resultingText;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getResultingText() {
      return resultingText;
    }
  }
}

