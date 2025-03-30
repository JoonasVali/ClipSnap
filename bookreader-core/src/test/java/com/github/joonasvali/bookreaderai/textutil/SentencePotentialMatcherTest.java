package com.github.joonasvali.bookreaderai.textutil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SentencePotentialMatcherTest {

  private final SentencePotentialMatcher matcher = new SentencePotentialMatcher();

  @Test
  public void testExactMatch() {
    String s1 = "Hello, world!";
    String s2 = "Hello, world!";
    SentencePotentialMatcher.MatchResult result = matcher.match(s1, s2);
    assertEquals(1.0, result.score, 0.001, "Exact match should return 1.0");
    assertEquals(s1, result.commonPart, "Common part should be the entire normalized string");
    assertEquals("", result.prefix, "There should be no prefix for an exact match");
    assertEquals("", result.suffix, "There should be no suffix for an exact match");

    assertEquals("", result.prefix);
    assertEquals("Hello, world!", result.commonPart);
    assertEquals("", result.suffix);
  }

  @Test
  public void testEllipsisMatch() {
    // "DOG CAT BIR…" should match "DOG CAT BIRD"
    String s1 = "DOG CAT BIR...";
    String s2 = "DOG CAT BIRD";
    SentencePotentialMatcher.MatchResult result = matcher.match(s1, s2);
    SentencePotentialMatcher.MatchResult result2 = matcher.match(s2, s1);
    assertTrue(result.score >= 0.9,
        "Ellipsis match should return a high score, got: " + result.score);

    assertEquals("", result.prefix);
    assertEquals("DOG CAT BIR...", result.commonPart);
    assertEquals("", result.suffix);

    assertEquals("", result2.prefix);
    assertEquals("DOG CAT BIR...", result2.commonPart);
    assertEquals("", result2.suffix);
  }

  @Test
  public void testLongestCommonSubstringFuzzyMatch() {
    // "Hello, world!" vs "world" should share the substring "world"
    String s1 = "Hello, world!";
    String s2 = "world";
    SentencePotentialMatcher.MatchResult result = matcher.match(s1, s2);
    // "world" normalized is "world"; expected fuzzy score from containment or LCS is at least 0.5.
    assertTrue(result.score >= 0.5,
        "Expected fuzzy match score > 0.5 based on LCS/containment, got: " + result.score);
    // Optionally, check that the common part equals "world" (normalized).
    assertEquals("world", result.commonPart, "Common part should be 'world'");
  }

  @Test
  public void testNoMatch() {
    String s1 = "abc";
    String s2 = "def";
    SentencePotentialMatcher.MatchResult result = matcher.match(s1, s2);
    assertEquals(0.0, result.score, 0.001, "No matching content should return 0.0");
  }

  @Test
  public void testNoMatchLonger() {
    String s1 = "Way to go.";
    String s2 = "Rest in peace.";
    SentencePotentialMatcher.MatchResult result = matcher.match(s1, s2);
    assertTrue(result.score < 0.08,
        "Expected fuzzy match score < 0.08 based on LCS/containment, got: " + result.score);
  }

  @Test
  public void testNoMatchLongest() {
    String s1 = "This is my reasoning of why I think this is a good idea.";
    String s2 = "World is a dangerous place to live; not because of the people who are evil, but because of the people who don't do anything about it.";
    SentencePotentialMatcher.MatchResult result = matcher.match(s1, s2);
    SentencePotentialMatcher.MatchResult result2 = matcher.match(s2, s1);
    assertTrue(result.score < 0.05,
        "Expected fuzzy match score < 0.05 based on LCS/containment, got: " + result.score);

    assertEquals("This is my reasoning of why I think this", result.prefix);
    assertEquals(" is a ", result.commonPart);
    assertEquals("dangerous place to live; not because of the people who are evil, but because of the people who don't do anything about it.", result.suffix);

    assertEquals("World", result2.prefix);
    assertEquals(" is a ", result2.commonPart);
    assertEquals("good idea.", result2.suffix);
  }

  @Test
  public void testEmptyString() {
    String s1 = "";
    String s2 = "something";
    SentencePotentialMatcher.MatchResult result = matcher.match(s1, s2);
    assertEquals(0.0, result.score, 0.001, "Empty string should result in 0.0 score");
  }


  @Test
  public void testShortNoMatch() {
    String s1 = "\n...\n";
    String s2 = "a.\n";
    SentencePotentialMatcher.MatchResult result = matcher.match(s1, s2);
    assertEquals(0.0, result.score, 0.001, "Short non match should result in 0.0 score");
  }

  @Test
  public void testShortNoMatchJustLetter() {
    String s1 = "a";
    String s2 = "b";
    SentencePotentialMatcher.MatchResult result = matcher.match(s1, s2);
    assertEquals(0.0, result.score, 0.001, "Short non match should result in 0.0 score");
  }

  @Test
  public void testShortNoMatchJustLetterWithPunctuationAndNewLine() {
    String s1 = "\na...";
    String s2 = "\nb...";
    SentencePotentialMatcher.MatchResult result = matcher.match(s1, s2);
    assertEquals(0.0, result.score, 0.001, "Short non match should result in 0.0 score");
  }

  @Test
  public void testShortNoMatchJustLetterWithPunctuation() {
    String s1 = "a...";
    String s2 = "b...";
    SentencePotentialMatcher.MatchResult result = matcher.match(s1, s2);
    assertEquals(0.0, result.score, 0.001, "Short non match should result in 0.0 score");
  }

  @Test
  public void testSubSentence() {
    String s1 = "While we wait I can't believe you are standing there\nThis is a serious matter.";
    String s2 = "This is a serious matter.";
    SentencePotentialMatcher.MatchResult result1 = matcher.match(s1, s2);
    SentencePotentialMatcher.MatchResult result2 = matcher.match(s2, s1);

    assertTrue(result1.score >= 0.9, "Subsentence match should return a high score, got: " + result1.score);
    assertTrue(result2.score >= 0.9, "Subsentence match should return a high score, got: " + result2.score);

    // Also, the common part should equal the normalized contained sentence.

    assertEquals("While we wait I can't believe you are standing there\n", result1.prefix, "Prefix should match");
    assertEquals("This is a serious matter.", result1.commonPart);
    assertEquals("", result1.suffix, "Suffix should be empty");

    assertEquals("This is a serious matter.", result2.commonPart);
    assertEquals("", result2.suffix, "Suffix should be empty");
    assertEquals("", result2.prefix, "Prefix should be empty");
  }

  @Test
  public void testSubSentencePartialMatch() {
    String s1 = "While we wait I can't believe you are standing there\nThis is a serious matter.";
    String s2 = "This is a se-erious matter.";
    SentencePotentialMatcher.MatchResult result1 = matcher.match(s1, s2);
    SentencePotentialMatcher.MatchResult result2 = matcher.match(s2, s1);

    assertTrue(result1.score >= 0.9, "Partial subsentence match should return a high score, got: " + result1.score);
    assertTrue(result2.score >= 0.9, "Partial subsentence match should return a high score, got: " + result2.score);

    assertEquals("While we wait I can't believe you are standing there\n", result1.prefix, "Prefix should match");
    assertEquals("", result1.suffix, "Suffix should be empty");
    assertEquals("This is a se-erious matter.", result1.commonPart);

    assertEquals("This is a se-erious matter.", result2.commonPart);
    assertEquals("", result2.suffix, "Suffix should be empty");
    assertEquals("", result2.prefix, "Prefix should be empty");
  }

  @Test
  public void testMissingOrExtraWord() {
    String s1 = "Gods are not to be mocked.";
    String s2 = "Gods are to be mocked.";
    SentencePotentialMatcher.MatchResult result1 = matcher.match(s1, s2);
    SentencePotentialMatcher.MatchResult result2 = matcher.match(s2, s1);

    assertTrue(result1.score >= 0.5, "Missing word match should return in a higher score, got: " + result1.score);
    assertTrue(result2.score >= 0.5, "Missing word match should return in a higher score, got: " + result2.score);

    assertEquals("Gods are not", result1.prefix);
    assertEquals(" to be mocked", result1.commonPart);
    assertEquals(".", result1.suffix);

    assertEquals("Gods are", result2.prefix);
    assertEquals(" to be mocked", result2.commonPart);
    assertEquals(".", result2.suffix);
  }

  @Test
  public void testMistypedWord() {
    String s1 = "Gods are not to be mocked.";
    String s2 = "Gods are knot to be mocked!";
    SentencePotentialMatcher.MatchResult result1 = matcher.match(s1, s2);
    SentencePotentialMatcher.MatchResult result2 = matcher.match(s2, s1);

    assertTrue(result1.score >= 0.9, "Mistyped word should have a higher score, got: " + result1.score);
    assertTrue(result2.score >= 0.9, "Mistyped word should have a higher score, got: " + result2.score);

    assertEquals("", result1.prefix);
    assertEquals("Gods are not to be mocked.", result1.commonPart);
    assertEquals("", result1.suffix);

    assertEquals("", result2.prefix);
    assertEquals("Gods are not to be mocked.", result2.commonPart);
    assertEquals("", result2.suffix);
  }

  @Test
  public void testPunctuation() {
    String s1 = "Gods are not to be mocked!";
    String s2 = "Gods are, not to: be mocked.";
    SentencePotentialMatcher.MatchResult result1 = matcher.match(s1, s2);
    SentencePotentialMatcher.MatchResult result2 = matcher.match(s2, s1);

    assertTrue(result1.score >= 0.9, "Mistyped word should have a higher score, got: " + result1.score);
    assertTrue(result2.score >= 0.9, "Mistyped word should have a higher score, got: " + result2.score);

    assertEquals("", result1.prefix);
    assertEquals("Gods are not to be mocked!", result1.commonPart);
    assertEquals("", result1.suffix);

    assertEquals("", result2.prefix);
    assertEquals("Gods are, not to: be mocked.", result2.commonPart);
    assertEquals("", result2.suffix);
  }

  @Test
  public void testVeryShortInputs() {
    String s1 = "The birds are singing.\n";
    String s2 = ".a,";
    SentencePotentialMatcher.MatchResult result1 = matcher.match(s1, s2);
    SentencePotentialMatcher.MatchResult result2 = matcher.match(s2, s1);

    assertTrue(result1.score < 0.1, "Got too high score: " + result1.score);
    assertTrue(result2.score < 0.1, "Got too high score: " + result2.score);
  }

  @Test
  public void testVeryShortInputs2() {
    String s1 = "Silence, the birds are singing.\n";
    String s2 = "s";
    SentencePotentialMatcher.MatchResult result1 = matcher.match(s1, s2);
    SentencePotentialMatcher.MatchResult result2 = matcher.match(s2, s1);

    assertEquals("S", result1.commonPart);
    assertEquals("s", result2.commonPart);
    assertEquals("", result1.prefix);
    assertEquals("", result1.suffix);
    assertEquals("", result2.prefix);
    assertEquals("ilence, the birds are singing.\n", result2.suffix);

    assertTrue(result1.score < 0.1, "Got too high score: " + result1.score);
    assertTrue(result2.score < 0.1, "Got too high score: " + result2.score);


  }
}
