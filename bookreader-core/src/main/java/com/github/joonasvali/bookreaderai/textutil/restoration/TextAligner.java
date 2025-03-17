package com.github.joonasvali.bookreaderai.textutil.restoration;

import java.util.HashMap;
import java.util.Map;

/**
 * The TextAligner class is responsible for aligning multiple text inputs.
 * Given an array of String texts, it computes an aligned (or consensus) text
 * using a word-by-word majority vote approach.
 */
public class TextAligner {

  /**
   * Encapsulates the result of an alignment operation.
   * It contains the aligned text and optional meta information.
   */
  public static class AlignmentResult {
    private final String alignedText;
    private final boolean success;

    public AlignmentResult(String alignedText, boolean success) {
      this.alignedText = alignedText;
      this.success = success;
    }

    public String getAlignedText() {
      return alignedText;
    }

    public boolean isSuccess() {
      return success;
    }
  }

  /**
   * Aligns the provided texts using a majority vote strategy.
   * <p>
   * Assumptions:
   * <ul>
   *   <li>Each text is pre-normalized (i.e., similar punctuation, consistent spacing, etc.).</li>
   *   <li>Alignment is performed on a word-by-word basis.</li>
   *   <li>If a particular word position is missing in one text, that text is simply skipped for that position.</li>
   * </ul>
   *
   * @param texts an array of texts to align
   * @return an AlignmentResult containing the aligned text and meta information about the operation
   */
  public AlignmentResult alignTexts(String[] texts) {
    // Validate input
    if (texts == null || texts.length == 0) {
      return new AlignmentResult("", false);
    }

    // Split each text into words (assuming words are separated by whitespace).
    // This assumes texts are already normalized for punctuation and casing.
    int maxWords = 0;
    String[][] wordsPerText = new String[texts.length][];
    for (int i = 0; i < texts.length; i++) {
      if (texts[i] != null) {
        wordsPerText[i] = texts[i].trim().split("\\s+");
        maxWords = Math.max(maxWords, wordsPerText[i].length);
      } else {
        wordsPerText[i] = new String[0];
      }
    }

    StringBuilder alignedTextBuilder = new StringBuilder();

    // Iterate over each word position.
    for (int wordIndex = 0; wordIndex < maxWords; wordIndex++) {
      Map<String, Integer> frequency = new HashMap<>();

      // Count frequency of each word at the current position.
      for (int i = 0; i < texts.length; i++) {
        if (wordIndex < wordsPerText[i].length) {
          // Optionally, further normalize the word (e.g., lower-case).
          String word = wordsPerText[i][wordIndex].toLowerCase();
          frequency.put(word, frequency.getOrDefault(word, 0) + 1);
        }
      }

      // Determine the majority word.
      String majorityWord = "";
      int maxCount = 0;
      for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
        if (entry.getValue() > maxCount) {
          majorityWord = entry.getKey();
          maxCount = entry.getValue();
        }
      }

      // Append the selected word to the output.
      alignedTextBuilder.append(majorityWord);
      if (wordIndex < maxWords - 1) {
        alignedTextBuilder.append(" ");
      }
    }

    return new AlignmentResult(alignedTextBuilder.toString(), true);
  }
}
