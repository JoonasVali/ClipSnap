package com.github.joonasvali.bookreaderai.textutil;

import java.util.HashMap;
import java.util.Map;

public class StringSelector {

  public String getMostSimilar(String[] texts) {
    // Maps to hold frequency, first index of occurrence, and the original content for each normalized content.
    Map<String, Integer> frequency = new HashMap<>();
    Map<String, Integer> firstIndex = new HashMap<>();
    Map<String, String> firstOccurrence = new HashMap<>();

    for (int i = 0; i < texts.length; i++) {
      String original = texts[i];
      // Normalize the content: trim and replace any sequence of whitespace with a single space.
      String normalized = original.trim().replaceAll("\\s+", " ");

      // Update frequency count and record the first occurrence if not seen before.
      if (!frequency.containsKey(normalized)) {
        frequency.put(normalized, 1);
        firstIndex.put(normalized, i);
        firstOccurrence.put(normalized, original);
      } else {
        frequency.put(normalized, frequency.get(normalized) + 1);
      }
    }

    // Determine the normalized content with the highest frequency.
    // In case of a tie, choose the one that appeared first.
    String chosenNormalized = null;
    int maxFrequency = 0;
    int chosenIndex = Integer.MAX_VALUE;

    for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
      String norm = entry.getKey();
      int freq = entry.getValue();
      int idx = firstIndex.get(norm);
      if (freq > maxFrequency || (freq == maxFrequency && idx < chosenIndex)) {
        maxFrequency = freq;
        chosenNormalized = norm;
        chosenIndex = idx;
      }
    }

    // If no content appears more than once, return null.
    if (maxFrequency < 2 && texts.length > 1) {
      return null;
    }

    // Return the original content corresponding to the first occurrence of the chosen normalized content.
    return firstOccurrence.get(chosenNormalized);
  }
}