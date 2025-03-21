package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.textutil.restoration.Sentence;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextSentenceMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextJoiner {

  public String join(String text1, String text2) {
    // Remove trailing whitespace from text1, but keep internal newlines.
    String t1 = text1.stripTrailing();
    // Use text2 as-is so that any embedded newlines are preserved.
    String t2 = text2;

    // 1) Sentence-level joining
    Sentence[] sentences1 = new TextSentenceMatcher().getSentences(t1);
    Sentence[] sentences2 = new TextSentenceMatcher().getSentences(t2);

    int commonIndexFirst = -1;
    int commonIndexSecond = -1;
    // Find the last matching sentence pair
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
      // Build the result:
      // From text1: all sentences up to (and including) the common sentence.
      List<String> resultSentencesT1 = new ArrayList<>();
      for (int i = 0; i <= commonIndexFirst; i++) {
        resultSentencesT1.add(sentences1[i].texts()[0]);
      }
      // From text2: all sentences after the common sentence.
      List<String> resultSentencesT2 = new ArrayList<>();
      for (int j = commonIndexSecond + 1; j < sentences2.length; j++) {
        resultSentencesT2.add(sentences2[j].texts()[0]);
      }

      // Merge the common sentence if text2's version is longer.
      String commonT1 = resultSentencesT1.get(resultSentencesT1.size() - 1);
      String commonT2 = sentences2[commonIndexSecond].texts()[0];
      String mergedCommon = mergeCommonSentence(commonT1, commonT2);
      resultSentencesT1.set(resultSentencesT1.size() - 1, mergedCommon);

      String joinedPart1 = joinWithMinimalExtraSpaces(resultSentencesT1);
      String joinedPart2 = joinWithMinimalExtraSpaces(resultSentencesT2);

      String joinedSentences;
      if (endsWithNewline(joinedPart1) || startsWithNewline(joinedPart2)) {
        joinedSentences = joinedPart1 + joinedPart2;
      } else if (!joinedPart1.isEmpty() && !joinedPart2.isEmpty()) {
        joinedSentences = joinedPart1 + " " + joinedPart2;
      } else {
        joinedSentences = joinedPart1 + joinedPart2;
      }

      return collapseSpacesWithinLines(joinedSentences);
    }

    // 2) Fallback: character-based overlap join
    // --- NEW CODE: Adjust t2 if it starts with duplicate content.
    String adjustedT2 = t2;
    String lastWordT1 = extractLastWord(t1);
    // Pattern matches optional whitespace, an optional "AND" plus whitespace, then a word, then the rest.
    Pattern p = Pattern.compile("^\\s*(?:AND\\s+)?(\\S+)(.*)$", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(adjustedT2);
    if (m.find()) {
      String firstWord = m.group(1);
      if (firstWord.equalsIgnoreCase(lastWordT1)) {
        adjustedT2 = m.group(2); // remove the duplicate prefix
      }
    }

    int overlapLength = findOverlap(t1, adjustedT2);
    String overlapPart = adjustedT2.substring(overlapLength);
    String joined = t1;
    if (!t1.isEmpty() && !overlapPart.isEmpty()) {
      char lastCharT1 = t1.charAt(t1.length() - 1);
      char firstCharOverlap = overlapPart.charAt(0);
      if (!Character.isWhitespace(lastCharT1) && !Character.isWhitespace(firstCharOverlap)) {
        joined += " ";
      }
    }
    joined += overlapPart;

    return collapseSpacesWithinLines(joined);
  }

  /**
   * Merges the two versions of the common sentence.
   * If the normalized version of s2 starts with s1 (ignoring case, whitespace, and punctuation)
   * and is longer, then s2 is considered the fuller version and is returned.
   * Otherwise, s1 is returned.
   */
  private String mergeCommonSentence(String s1, String s2) {
    String n1 = normalize(s1);
    String n2 = normalize(s2);
    if (n2.startsWith(n1) && s2.length() > s1.length()) {
      return s2;
    }
    return s1;
  }

  private String joinWithMinimalExtraSpaces(List<String> sentences) {
    StringBuilder sb = new StringBuilder();
    for (String sentence : sentences) {
      // Remove leading spaces (but preserve newlines)
      sentence = sentence.replaceFirst("^[ ]+", "");
      if (sb.length() == 0) {
        sb.append(sentence);
      } else {
        if (endsWithNewline(sb)) {
          sb.append(sentence);
        } else if (!endsWithWhitespaceOrNewline(sb) && !startsWithWhitespaceOrNewline(sentence)) {
          sb.append(' ');
          sb.append(sentence);
        } else {
          sb.append(sentence);
        }
      }
    }
    return sb.toString();
  }

  private boolean endsWithNewline(String text) {
    if (text.isEmpty()) return false;
    char c = text.charAt(text.length() - 1);
    return c == '\n' || c == '\r';
  }

  private boolean endsWithNewline(StringBuilder sb) {
    return endsWithNewline(sb.toString());
  }

  private boolean endsWithWhitespaceOrNewline(StringBuilder sb) {
    if (sb.length() == 0) return true;
    char c = sb.charAt(sb.length() - 1);
    return Character.isWhitespace(c);
  }

  private boolean startsWithNewline(String str) {
    if (str == null || str.isEmpty()) {
      return false;
    }
    char first = str.charAt(0);
    return first == '\n' || first == '\r';
  }

  private boolean startsWithWhitespaceOrNewline(String str) {
    if (str.isEmpty()) return true;
    return Character.isWhitespace(str.charAt(0));
  }

  /**
   * Collapses multiple consecutive spaces into a single space within each line,
   * preserving line breaks as-is. Also removes any leading spaces on each line.
   */
  private String collapseSpacesWithinLines(String text) {
    String[] lines = text.split("\r?\n", -1);
    for (int i = 0; i < lines.length; i++) {
      lines[i] = lines[i].replaceFirst("^ +", ""); // remove leading spaces
      lines[i] = lines[i].replaceAll(" +", " ");  // collapse multiple spaces
    }
    return String.join("\n", lines);
  }

  private boolean sentencesMatch(String s1, String s2) {
    if (s1 == null || s2 == null) return false;
    String n1 = normalize(s1);
    String n2 = normalize(s2);
    // Allow exact match or prefix match.
    return n1.equals(n2) || n1.startsWith(n2) || n2.startsWith(n1);
  }

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

  private boolean normalizedEquals(String x, String y) {
    return normalize(x).equals(normalize(y));
  }

  private String normalize(String s) {
    return s.toLowerCase().replaceAll("[\\s\\p{Punct}]+", "");
  }

  // NEW HELPER: extracts the last word from a string (splitting on whitespace)
  private String extractLastWord(String s) {
    s = s.trim();
    String[] parts = s.split("\\s+");
    return parts[parts.length - 1];
  }
}
