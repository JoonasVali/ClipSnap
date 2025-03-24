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
   * A simple structure holding a token’s base (the word without any trailing newline)
   * and a flag indicating if the token originally ended with a newline.
   */
  private static class TokenInfo {
    String base;      // the token without a trailing newline
    boolean hasNewline;  // whether the token ended with a newline

    TokenInfo(String token) {
      if (token.endsWith("\n")) {
        this.base = token.substring(0, token.length() - 1);
        this.hasNewline = true;
      } else {
        this.base = token;
        this.hasNewline = false;
      }
    }
  }

  /**
   * Aligns the provided texts using a majority vote strategy.
   * <p>
   * This implementation treats line breaks as part of the token.
   * It splits each text using a single space (" ") so that tokens containing "\n" keep the newline
   * attached. When comparing tokens, it uses the base word (token without trailing newline)
   * in a case-insensitive manner. After computing the majority base word at each position,
   * it chooses one token variant (preserving its original case) from among those matching the majority.
   * If any of those tokens had a trailing newline, the resulting token will have one appended.
   * <p>
   * Reassembly inserts a space between tokens unless the previous token ended with a newline;
   * in that case, if the token is not the last token, an extra space is appended immediately after the newline.
   *
   * @param texts an array of texts to align
   * @return an AlignmentResult containing the aligned text and meta information about the operation
   */
  public VoteResult vote(String[] texts) {
    if (texts == null || texts.length == 0) {
      return new VoteResult("", false);
    }

    // Split each text into tokens by a single space.
    // This preserves newline characters as part of tokens.
    int maxTokens = 0;
    TokenInfo[][] tokensPerText = new TokenInfo[texts.length][];
    for (int i = 0; i < texts.length; i++) {
      if (texts[i] != null) {
        String[] rawTokens = texts[i].split(" ");
        tokensPerText[i] = new TokenInfo[rawTokens.length];
        for (int j = 0; j < rawTokens.length; j++) {
          tokensPerText[i][j] = new TokenInfo(rawTokens[j]);
        }
        maxTokens = Math.max(maxTokens, rawTokens.length);
      } else {
        tokensPerText[i] = new TokenInfo[0];
      }
    }

    // For each token position, do a majority vote on the base word (ignoring newline flag).
    // Also track if any token for that base word contained a newline.
    String[] alignedTokens = new String[maxTokens];
    for (int tokenIndex = 0; tokenIndex < maxTokens; tokenIndex++) {
      Map<String, Integer> frequency = new HashMap<>();
      Map<String, Boolean> newlineFlag = new HashMap<>();

      // Count frequencies and record newline flags for each base word (using lower-case for counting).
      for (int i = 0; i < texts.length; i++) {
        if (tokenIndex < tokensPerText[i].length) {
          TokenInfo tokenInfo = tokensPerText[i][tokenIndex];
          String baseLower = tokenInfo.base.toLowerCase();
          frequency.put(baseLower, frequency.getOrDefault(baseLower, 0) + 1);
          // Mark newline flag true if any token has a newline.
          if (tokenInfo.hasNewline) {
            newlineFlag.put(baseLower, true);
          } else {
            newlineFlag.putIfAbsent(baseLower, false);
          }
        }
      }

      // Determine the majority base word.
      String majorityBaseLower = "";
      int maxCount = 0;
      for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
        if (entry.getValue() > maxCount) {
          majorityBaseLower = entry.getKey();
          maxCount = entry.getValue();
        }
      }

      // Choose a candidate token variant from the texts that has the majority base (preserving original case).
      String candidate = "";
      for (int i = 0; i < texts.length; i++) {
        if (tokenIndex < tokensPerText[i].length) {
          TokenInfo tokenInfo = tokensPerText[i][tokenIndex];
          if (tokenInfo.base.toLowerCase().equals(majorityBaseLower)) {
            candidate = tokenInfo.base;
            break;
          }
        }
      }

      // Append newline if any token with this base had one.
      boolean majorityHasNewline = newlineFlag.getOrDefault(majorityBaseLower, false);
      alignedTokens[tokenIndex] = majorityHasNewline ? candidate + "\n" : candidate;
    }

    // Reassemble the aligned tokens into a single string.
    StringBuilder alignedTextBuilder = new StringBuilder();
    for (int i = 0; i < alignedTokens.length; i++) {
      String token = alignedTokens[i];
      if (i > 0 && !alignedTokens[i - 1].endsWith("\n")) {
        alignedTextBuilder.append(" ");
      }
      alignedTextBuilder.append(token);
      if (token.endsWith("\n") && i < alignedTokens.length - 1) {
        // If the token ends with a newline and it's not the final token,
        // append an extra space to preserve a leading space on the next line.
        alignedTextBuilder.append(" ");
      }
    }

    return new VoteResult(alignedTextBuilder.toString(), true);
  }
}