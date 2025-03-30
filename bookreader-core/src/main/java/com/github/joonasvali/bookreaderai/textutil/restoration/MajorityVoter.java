package com.github.joonasvali.bookreaderai.textutil.restoration;

import java.util.HashMap;
import java.util.Map;

/**
 * The TextAligner class is responsible for aligning multiple text inputs.
 * Given an array of String texts, it computes an aligned (or consensus) text
 * using a word-by-word majority vote approach.
 */
public class MajorityVoter {

  /**
   * Encapsulates the result of an alignment operation.
   * It contains the aligned text and optional meta information.
   */
  public static class VoteResult {
    private final String resultingText;
    private final boolean success;

    public VoteResult(String resultingText, boolean success) {
      this.resultingText = resultingText;
      this.success = success;
    }

    public String getResultingText() {
      return resultingText;
    }

    public boolean isSuccess() {
      return success;
    }
  }

  /**
   * A simple structure holding a token’s original text and a flag indicating if the token originally ended with a newline.
   */
  private static class TokenInfo {
    String token;      // the original token without the trailing newline
    boolean hasNewline;  // whether the token ended with a newline

    TokenInfo(String token) {
      if (token.endsWith("\n")) {
        this.token = token.substring(0, token.length() - 1);
        this.hasNewline = true;
      } else {
        this.token = token;
        this.hasNewline = false;
      }
    }
  }

  /**
   * Normalizes a token for voting purposes.
   * Converts to lower-case and, if it ends with a period or comma, strips that trailing character.
   */
  private String normalize(String token) {
    String norm = token.toLowerCase();
    if (norm.endsWith(".") || norm.endsWith(",")) {
      norm = norm.substring(0, norm.length() - 1);
    }
    return norm;
  }

  /**
   * Aligns the provided texts using a majority vote strategy.
   * <p>
   * The algorithm splits texts into tokens (separated by a single space), preserving newlines.
   * It uses the first non-null, non-empty text as the reference.
   * For each token position (up to the reference token count):
   * - For non-final tokens: if the reference token ends with punctuation (comma or period), it is trusted and used directly.
   * - For the final token: a majority vote is performed and then, if at least one text provided a token without punctuation,
   *   a trailing period (or comma) is stripped from the chosen candidate.
   * Token voting is done by normalizing tokens (to lower-case and stripping trailing punctuation)
   * and counting frequencies. In the event the majority candidate appears strictly more often than the reference token,
   * that candidate is chosen; otherwise, the reference token is used.
   * After a token is chosen, if the reference token originally ended with a newline,
   * a newline is appended.
   * Reassembly inserts spaces between tokens unless the preceding token ended with a newline.
   *
   * @param texts an array of texts to align
   * @return a VoteResult containing the aligned text and meta information about the operation
   */
  public VoteResult vote(String[] texts) {
    if (texts == null || texts.length == 0) {
      return new VoteResult("", false);
    }

    // Use the first non-null, non-empty text as the reference.
    String refText = null;
    int refIndex = -1;
    for (int i = 0; i < texts.length; i++) {
      if (texts[i] != null && !texts[i].isEmpty()) {
        refText = texts[i];
        refIndex = i;
        break;
      }
    }
    if (refText == null) {
      return new VoteResult("", false);
    }

    // Split each text into tokens by a single space.
    int nTexts = texts.length;
    TokenInfo[][] tokensPerText = new TokenInfo[nTexts][];
    for (int i = 0; i < nTexts; i++) {
      if (texts[i] != null && !texts[i].isEmpty()) {
        String[] rawTokens = texts[i].split(" ");
        tokensPerText[i] = new TokenInfo[rawTokens.length];
        for (int j = 0; j < rawTokens.length; j++) {
          tokensPerText[i][j] = new TokenInfo(rawTokens[j]);
        }
      } else {
        tokensPerText[i] = new TokenInfo[0];
      }
    }

    // Use the reference token array length as our limit.
    TokenInfo[] refTokens = tokensPerText[refIndex];
    int refLength = refTokens.length;
    String[] alignedTokens = new String[refLength];

    for (int pos = 0; pos < refLength; pos++) {
      String refToken = refTokens[pos].token;
      String refNorm = normalize(refToken);
      String chosen;

      // For non-final tokens, if the reference token ends with punctuation, trust it.
      if (pos != refLength - 1 && (refToken.endsWith(",") || refToken.endsWith("."))) {
        chosen = refToken;
      } else {
        // Build frequency and candidate maps from available tokens at this position.
        Map<String, Integer> frequency = new HashMap<>();
        Map<String, String> candidateMap = new HashMap<>();
        for (int t = 0; t < nTexts; t++) {
          if (pos < tokensPerText[t].length) {
            String token = tokensPerText[t][pos].token;
            String norm = normalize(token);
            frequency.put(norm, frequency.getOrDefault(norm, 0) + 1);
            // Choose candidate variant: prefer one without trailing punctuation.
            if (!candidateMap.containsKey(norm)) {
              candidateMap.put(norm, token);
            } else {
              String existing = candidateMap.get(norm);
              if ((existing.endsWith(".") || existing.endsWith(",")) && !(token.endsWith(".") || token.endsWith(","))) {
                candidateMap.put(norm, token);
              }
            }
          }
        }

        // Determine the majority normalized candidate.
        String majorityNorm = refNorm;
        int maxCount = frequency.getOrDefault(refNorm, 0);
        for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
          if (entry.getValue() > maxCount) {
            majorityNorm = entry.getKey();
            maxCount = entry.getValue();
          }
        }
        int refCount = frequency.getOrDefault(refNorm, 0);
        // If the majority candidate appears strictly more than the reference token, choose it.
        if (maxCount > refCount && candidateMap.containsKey(majorityNorm)) {
          chosen = candidateMap.get(majorityNorm);
        } else {
          chosen = refToken;
        }
      }

      // For the final token, if any text's token did not end with punctuation, then strip trailing punctuation from the chosen token.
      if (pos == refLength - 1) {
        boolean hasNonPunctuatedVariant = false;
        for (int t = 0; t < nTexts; t++) {
          if (pos < tokensPerText[t].length) {
            String token = tokensPerText[t][pos].token;
            if (!(token.endsWith(".") || token.endsWith(","))) {
              hasNonPunctuatedVariant = true;
              break;
            }
          }
        }
        if (hasNonPunctuatedVariant && (chosen.endsWith(".") || chosen.endsWith(","))) {
          chosen = chosen.substring(0, chosen.length() - 1);
        }
      }

      // Preserve the trailing newline if the reference token originally had one.
      if (refTokens[pos].hasNewline && !chosen.endsWith("\n")) {
        chosen = chosen + "\n";
      }
      alignedTokens[pos] = chosen;
    }

    // Reassemble tokens with proper spacing.
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < alignedTokens.length; i++) {
      if (i > 0 && !alignedTokens[i - 1].endsWith("\n")) {
        sb.append(" ");
      }
      sb.append(alignedTokens[i]);
      if (alignedTokens[i].endsWith("\n") && i < alignedTokens.length - 1) {
        sb.append(" ");
      }
    }

    return new VoteResult(sb.toString(), true);
  }
}
