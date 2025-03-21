package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.textutil.restoration.Sentence;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextSentenceMatcher;

import java.util.ArrayList;
import java.util.List;

public class TextJoiner {

  /*
   * Comment: Sentence is just a record: public record Sentence(String[] texts) { }
   */
  public String join(String text1, String text2) {
    // Trim inputs
    String t1 = text1.trim();
    String t2 = text2.trim();

    // 1) Try sentence-level joining
    Sentence[] sentences1 = new TextSentenceMatcher().getSentences(t1);
    Sentence[] sentences2 = new TextSentenceMatcher().getSentences(t2);

    int commonIndexFirst = -1;
    int commonIndexSecond = -1;
    // Instead of breaking on the first match, iterate over all pairs so that the last matching pair is recorded.
    for (int i = 0; i < sentences1.length; i++) {
      String s1 = sentences1[i].texts()[0];
      for (int j = 0; j < sentences2.length; j++) {
        String s2 = sentences2[j].texts()[0];
        if (sentencesMatch(s1, s2)) {
          commonIndexFirst = i;
          commonIndexSecond = j;
        }
      }
    }

    if (commonIndexFirst != -1 && commonIndexSecond != -1) {
      // We found a common sentence; build the result by taking all sentences
      // up to the common sentence from t1, then the rest from t2.
      List<String> resultSentences = new ArrayList<>();
      for (int i = 0; i <= commonIndexFirst; i++) {
        resultSentences.add(sentences1[i].texts()[0]);
      }
      for (int j = commonIndexSecond + 1; j < sentences2.length; j++) {
        resultSentences.add(sentences2[j].texts()[0]);
      }
      // Carefully join them, preserving line breaks within each sentence.
      String joinedSentences = joinWithMinimalExtraSpaces(resultSentences);
      return collapseSpacesWithinLines(joinedSentences).trim();
    }

    // 2) Fallback: character-based overlap join
    int overlapLength = findOverlap(t1, t2);
    String overlapPart = t2.substring(overlapLength);
    String joined = t1;
    if (!t1.isEmpty() && !overlapPart.isEmpty()) {
      char lastCharT1 = t1.charAt(t1.length() - 1);
      char firstCharOverlap = overlapPart.charAt(0);
      if (!Character.isWhitespace(lastCharT1) && !Character.isWhitespace(firstCharOverlap)) {
        joined += " ";
      }
    }
    joined += overlapPart;

    // Normalize spacing within lines (but preserve line breaks)
    return collapseSpacesWithinLines(joined).trim();
  }

  /**
   * Joins multiple sentence strings by inserting a single space only if
   * the end of the current result and the start of the next sentence
   * are not already separated by whitespace/newlines.
   */
  private String joinWithMinimalExtraSpaces(List<String> sentences) {
    StringBuilder sb = new StringBuilder();
    for (String sentence : sentences) {
      if (sb.length() == 0) {
        // First sentence
        sb.append(sentence);
      } else {
        // If we don't already end with whitespace or newline, and the new sentence
        // doesn't start with whitespace/newline, then insert a single space
        if (!endsWithWhitespaceOrNewline(sb) && !startsWithWhitespaceOrNewline(sentence)) {
          sb.append(' ');
        }
        sb.append(sentence);
      }
    }
    return sb.toString();
  }

  private boolean endsWithWhitespaceOrNewline(StringBuilder sb) {
    if (sb.length() == 0) {
      return true;
    }
    char c = sb.charAt(sb.length() - 1);
    return Character.isWhitespace(c);
  }

  private boolean startsWithWhitespaceOrNewline(String str) {
    if (str.isEmpty()) {
      return true;
    }
    return Character.isWhitespace(str.charAt(0));
  }

  /**
   * Collapses multiple consecutive spaces into a single space **within each line**,
   * preserving line breaks as-is. Also removes any leading spaces on each line.
   */
  private String collapseSpacesWithinLines(String text) {
    // Split into lines, fix spacing in each line, then rejoin
    String[] lines = text.split("\r?\n", -1);
    for (int i = 0; i < lines.length; i++) {
      // remove leading spaces
      lines[i] = lines[i].replaceFirst("^ +", "");
      // collapse multiple spaces
      lines[i] = lines[i].replaceAll(" +", " ");
    }
    return String.join("\n", lines);
  }

  /**
   * Returns true if two sentences match when normalized (ignoring case, extra whitespace and punctuation).
   */
  private boolean sentencesMatch(String s1, String s2) {
    if (s1 == null || s2 == null) {
      return false;
    }
    return normalize(s1).equals(normalize(s2));
  }

  /**
   * Finds the maximum overlap length between the end of string 'a' and the beginning of string 'b'
   * by comparing normalized substrings.
   */
  private int findOverlap(String a, String b) {
    int max = Math.min(a.length(), b.length());
    for (int k = max; k > 0; k--) {
      String subA = a.substring(a.length() - k);
      String subB = b.substring(0, k);
      if (normalizedEquals(subA, subB)) {
        return k;
      }
    }
    return 0;
  }

  /**
   * Compares two strings after normalization.
   */
  private boolean normalizedEquals(String x, String y) {
    return normalize(x).equals(normalize(y));
  }

  /**
   * Normalizes the string by converting it to lowercase and removing whitespace and punctuation.
   */
  private String normalize(String s) {
    return s.toLowerCase().replaceAll("[\\s\\p{Punct}]+", "");
  }
}
