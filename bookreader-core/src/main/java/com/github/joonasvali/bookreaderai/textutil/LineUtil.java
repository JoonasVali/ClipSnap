package com.github.joonasvali.bookreaderai.textutil;

public class LineUtil {

  /**
   * Wraps the given content so that each line does not exceed maxWidth characters.
   * Existing line breaks are preserved.
   *
   * @param text     the content to wrap
   * @param maxWidth maximum number of characters per line
   * @return the word-wrapped content
   */
  public String lineBreakAfterEvery(String text, int maxWidth) {
    // Split the content into lines based on existing line breaks
    String[] lines = text.split("\\r?\\n");
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < lines.length; i++) {
      result.append(wrapLine(lines[i], maxWidth));
      if (i < lines.length - 1) {
        result.append("\n");
      }
    }
    return result.toString();
  }

  /**
   * Wraps a single line of content so that no line exceeds maxWidth characters.
   * Words are kept intact unless a word itself exceeds the maximum width.
   *
   * @param line     the line to wrap
   * @param maxWidth maximum number of characters per line
   * @return the wrapped line
   */
  private String wrapLine(String line, int maxWidth) {
    StringBuilder wrapped = new StringBuilder();
    String[] words = line.split(" ");
    int currentLineLength = 0;

    for (int i = 0; i < words.length; i++) {
      String word = words[i];

      // Skip any empty words (e.g. due to multiple spaces)
      if (word.isEmpty()) {
        continue;
      }

      // If the word itself is longer than maxWidth, we need to break it.
      if (word.length() > maxWidth) {
        // If there's already some content on the current line, start a new line.
        if (currentLineLength > 0) {
          wrapped.append("\n");
          currentLineLength = 0;
        }
        // Break the long word into chunks of maxWidth characters.
        while (word.length() > maxWidth) {
          wrapped.append(word, 0, maxWidth);
          word = word.substring(maxWidth);
          wrapped.append("\n");
        }
        // Append any leftover part of the word.
        wrapped.append(word);
        currentLineLength = word.length();
      } else {
        // If adding the word (with a preceding space if necessary) would exceed maxWidth,
        // start a new line.
        if (currentLineLength != 0 && currentLineLength + 1 + word.length() > maxWidth) {
          wrapped.append("\n");
          wrapped.append(word);
          currentLineLength = word.length();
        } else {
          if (currentLineLength != 0) {
            wrapped.append(" ");
            currentLineLength++; // count the space
          }
          wrapped.append(word);
          currentLineLength += word.length();
        }
      }
    }
    return wrapped.toString();
  }
}