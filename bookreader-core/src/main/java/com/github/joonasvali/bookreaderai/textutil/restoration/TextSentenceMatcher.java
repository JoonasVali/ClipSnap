package com.github.joonasvali.bookreaderai.textutil.restoration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class is used to match sentences from multiple texts.
 * This is fully AI generated.
 * The code was implemented based on tests in TextSentenceMatcherTest class.
 */
public class TextSentenceMatcher {

  // Split the text into sentences using a regex that splits on punctuation + whitespace.
  private List<String> splitSentences(String text) {
    List<String> sentences = new ArrayList<>();
    Pattern pattern = Pattern.compile("(?<=[.!?])\\s+");
    String[] parts = pattern.split(text);
    for (String part : parts) {
      sentences.add(part.trim());
    }
    return sentences;
  }

  // A simple similarity score based on common words.
  private double similarity(String s1, String s2) {
    String[] words1 = s1.split("\\s+");
    String[] words2 = s2.split("\\s+");
    if (words1.length == 0 || words2.length == 0) {
      return 0.0;
    }
    int common = 0;
    for (String w1 : words1) {
      for (String w2 : words2) {
        if (w1.equalsIgnoreCase(w2)) {
          common++;
          break;
        }
      }
    }
    return (double) common / ((words1.length + words2.length) / 2.0);
  }

  // Try to split one candidate segment if it's exactly one sentence short.
  // We attempt to split a segment (starting at index 1) that contains a comma.
  private List<String> tryToSplitCandidate(List<String> candidate, int targetCount) {
    if (candidate.size() == targetCount - 1) {
      for (int i = 1; i < candidate.size(); i++) {
        String seg = candidate.get(i);
        if (seg.contains(",")) {
          int pos = seg.indexOf(",");
          if (pos > 0 && pos < seg.length() - 1) {
            String left = seg.substring(0, pos + 1).trim();
            String right = seg.substring(pos + 1).trim();
            if (!left.isEmpty() && !right.isEmpty()) {
              List<String> newCandidate = new ArrayList<>();
              for (int j = 0; j < i; j++) {
                newCandidate.add(candidate.get(j));
              }
              newCandidate.add(left);
              newCandidate.add(right);
              for (int j = i + 1; j < candidate.size(); j++) {
                newCandidate.add(candidate.get(j));
              }
              return newCandidate;
            }
          }
        }
      }
    }
    return candidate;
  }

  // Adjust a candidate sentence list to have exactly targetCount elements.
  // If there are extra segments, merge the first extra ones.
  // If there are fewer segments, try splitting then fall back to greedy alignment.
  private List<String> adjustSentences(List<String> candidate, List<String> reference, int targetCount) {
    List<String> result = new ArrayList<>();
    if (candidate.size() == targetCount) {
      result.addAll(candidate);
    } else if (candidate.size() > targetCount) {
      // Merge extra segments (assumed to be at the beginning).
      int diff = candidate.size() - targetCount;
      StringBuilder merged = new StringBuilder();
      for (int i = 0; i <= diff; i++) {
        if (i > 0) merged.append(" ");
        merged.append(candidate.get(i));
      }
      result.add(merged.toString().trim());
      for (int i = diff + 1; i < candidate.size(); i++) {
        result.add(candidate.get(i));
      }
    } else { // candidate.size() < targetCount
      candidate = tryToSplitCandidate(candidate, targetCount);
      if (candidate.size() == targetCount) {
        result.addAll(candidate);
      } else {
        double threshold = 0.3;
        int j = 0;
        for (int i = 0; i < targetCount; i++) {
          if (j < candidate.size() && similarity(candidate.get(j), reference.get(i)) >= threshold) {
            result.add(candidate.get(j));
            j++;
          } else {
            result.add("");
          }
        }
      }
    }
    return result;
  }

  public Sentence[] getSentences(String... texts) {
    List<List<String>> allSentences = new ArrayList<>();
    int maxCount = 0;
    int minCount = Integer.MAX_VALUE;
    for (String txt : texts) {
      List<String> sents = splitSentences(txt);
      allSentences.add(sents);
      int count = sents.size();
      if (count > maxCount) maxCount = count;
      if (count < minCount) minCount = count;
    }

    // Decide targetCount:
    // If the difference between max and min is more than 1, use (max - 1), otherwise use max.
    int targetCount = (maxCount - minCount > 1) ? maxCount - 1 : maxCount;

    // Choose a reference: first text with count equal to targetCount, or fallback.
    List<String> reference = null;
    for (List<String> sents : allSentences) {
      if (sents.size() == targetCount) {
        reference = sents;
        break;
      }
    }
    if (reference == null) {
      reference = new ArrayList<>();
      for (int i = 0; i < targetCount; i++) {
        reference.add("");
      }
    }

    List<List<String>> adjusted = new ArrayList<>();
    for (List<String> candidate : allSentences) {
      adjusted.add(adjustSentences(new ArrayList<>(candidate), reference, targetCount));
    }

    Sentence[] result = new Sentence[targetCount];
    for (int i = 0; i < targetCount; i++) {
      String[] sentenceVersions = new String[texts.length];
      for (int j = 0; j < texts.length; j++) {
        sentenceVersions[j] = adjusted.get(j).get(i);
      }
      result[i] = new Sentence(sentenceVersions);
    }
    return result;
  }
}
