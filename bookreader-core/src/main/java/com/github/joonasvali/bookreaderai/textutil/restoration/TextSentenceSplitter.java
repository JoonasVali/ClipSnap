package com.github.joonasvali.bookreaderai.textutil.restoration;

import java.util.ArrayList;
import java.util.List;

/**

 A straightforward sentence splitter for a single text input.
 Splits text into sentences by looking at '.', '!', '?',
 and includes a following newline if present just after whitespace.
 It also trims leading and trailing whitespace appropriately. */
public class TextSentenceSplitter {
  /**

   Splits the input text into sentences.
   Linebreaks are treated as normal characters except when they occur
   immediately after punctuation as part of the following whitespace – then
   they are kept with the sentence ending in the punctuation. */
  private List<String> splitSentences(String text) {
    List<String> sentences = new ArrayList<>();
    int n = text.length();
    int start = 0;
    for (int i = 0; i < n; i++) {
      char c = text.charAt(i);
      // Check if c is a sentence-ending punctuation.
      if (c == '.' || c == '!' || c == '?') {
        // Advance i to include any consecutive punctuation characters.
        while (i + 1 < n && (text.charAt(i + 1) == '.' || text.charAt(i + 1) == '!' || text.charAt(i + 1) == '?')) {
          i++;
        }
        int j = i + 1;
        // Advance j while the characters are spaces or tabs.
        while (j < n && (text.charAt(j) == ' ' || text.charAt(j) == '\t')) {
          j++;
        }
        // If the very next character is a newline, include it.
        if (j < n && text.charAt(j) == '\n') {
          j++; // include the newline with the sentence.
        }
        // Extract the sentence from 'start' to j.
        String sentence = text.substring(start, j);
        sentences.add(sentence);
        start = j; // next sentence starts here.
        i = j - 1; // update i accordingly.
      }
    }
// Add any remaining text as a sentence.
    if (start < n) {
      sentences.add(text.substring(start));
    }

// Post-process each sentence:
// - Remove leading spaces/tabs from every sentence except the first.
// - If a sentence does not end with a newline, trim trailing whitespace.
    List<String> result = new ArrayList<>();
    for (int i = 0; i < sentences.size(); i++) {
      String s = sentences.get(i);
      if (i > 0) {
        s = s.replaceFirst("^[ \\t]+", "");
      }
      if (!s.endsWith("\n")) {
        s = s.replaceAll("[ \\t]+$", "");
      }
      result.add(s);
    }

    return result;
  }

  /**

   Returns all the sentences from the input text as an array. */
  public String[] getSentences(String text) {
    List<String> sentences = splitSentences(text);
    return sentences.toArray(new String[0]);
  }
}