package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.textutil.restoration.Sentence;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextSentenceMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextJoiner {

  // Use the fuzzy matcher for sentence comparison.
  private final SentencePotentialMatcher fuzzyMatcher = new SentencePotentialMatcher();
  // Threshold for considering two sentences a match.
  private static final double MATCH_THRESHOLD = 0.8;

  public String join(String text1, String text2) {
    // Remove trailing whitespace from text1 (but keep internal newlines).
    String t1 = text1.stripTrailing();
    // Use text2 as-is so that any embedded newlines are preserved.
    String t2 = text2;

    // 1) Sentence-level joining
    Sentence[] sentences1 = new TextSentenceMatcher().getSentences(t1);
    Sentence[] sentences2 = new TextSentenceMatcher().getSentences(t2);

    int commonIndexFirst = -1;
    int commonIndexSecond = -1;
    // Find the last matching sentence pair using fuzzy matching.
    for (int i = 0; i < sentences1.length; i++) {
      String s1 = sentences1[i].texts()[0];
      for (int j = 0; j < sentences2.length; j++) {
        String s2 = sentences2[j].texts()[0];
        // When s2 contains a newline, use only its first line for matching.
        String s2FirstLine = s2.contains("\n") ? s2.split("\n", 2)[0] : s2;
        double score = fuzzyMatcher.matchScore(s1, s2FirstLine);
        if (score >= MATCH_THRESHOLD) {
          // Later matches override earlier ones.
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

      // Process the common sentence:
      String commonT1 = resultSentencesT1.get(resultSentencesT1.size() - 1);
      String commonT2 = sentences2[commonIndexSecond].texts()[0];
      // Instead of splitting off only the first line from text2's sentence, we let mergeCommonSentence decide.
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
    String adjustedT2 = t2;
    String lastWordT1 = extractLastWord(t1);
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
   * If the fuzzy match score between s1 and s2 is at or above the threshold,
   * then we return s2 (i.e. the fuller version). Otherwise, we return s1.
   */
  private String mergeCommonSentence(String s1, String s2) {
    if (fuzzyMatcher.matchScore(s1, s2) >= MATCH_THRESHOLD) {
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

  // Helper: extracts the last word from a string (splitting on whitespace)
  private String extractLastWord(String s) {
    s = s.trim();
    String[] parts = s.split("\\s+");
    return parts[parts.length - 1];
  }
}
