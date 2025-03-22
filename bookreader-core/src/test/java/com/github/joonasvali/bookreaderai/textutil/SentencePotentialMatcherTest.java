package com.github.joonasvali.bookreaderai.textutil;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SentencePotentialMatcherTest {

  private final SentencePotentialMatcher matcher = new SentencePotentialMatcher();

  @Test
  public void testExactMatch() {
    String s1 = "Hello, world!";
    String s2 = "Hello, world!";
    double score = matcher.matchScore(s1, s2);
    assertEquals(1.0, score, 0.001, "Exact match should return 1.0");
  }

  @Test
  public void testPrefixMatch() {
    // "Hello my friends." and "my friends." share a prefix overlap (fuzzy)
    String s1 = "Hello my friends.";
    String s2 = "my friends.";
    double score = matcher.matchScore(s1, s2);
    assertTrue(score >= 0.6 && score < 0.8,
        "Prefix match should return a high score but less than 1.0, got: " + score);
  }

  @Test
  public void testEllipsisMatch() {
    // "DOG CAT BIR…" should match "DOG CAT BIRD"
    String s1 = "DOG CAT BIR…";
    String s2 = "DOG CAT BIRD";
    double score = matcher.matchScore(s1, s2);
    assertTrue(score >= 0.9,
        "Ellipsis match should return a high score, got: " + score);
  }

  @Test
  public void testLongestCommonSubstringFuzzyMatch() {
    // "Hello, world!" vs "world" should share the substring "world"
    String s1 = "Hello, world!";
    String s2 = "world";
    double score = matcher.matchScore(s1, s2);
    // "world" is 5 characters; with normalization s1 becomes "helloworld" (10 chars) so LCS ratio = 5/10 = 0.5.
    assertTrue(score >= 0.5,
        "Expected fuzzy match score > 0.5 based on LCS, got: " + score);
  }

  @Test
  public void testNoMatch() {
    String s1 = "abc";
    String s2 = "def";
    double score = matcher.matchScore(s1, s2);
    assertEquals(0.0, score, 0.001, "No matching content should return 0.0");
  }

  @Test
  public void testNoMatchLonger() {
    String s1 = "This is my reasoning of why I think this is a good idea.";
    String s2 = "World is a dangerous place to live; not because of the people who are evil, but because of the people who don't do anything about it.";
    double score = matcher.matchScore(s1, s2);
    assertTrue(score < 0.04,
        "Expected fuzzy match score < 0.04 based on LCS, got: " + score);
  }

  @Test
  public void testEmptyString() {
    String s1 = "";
    String s2 = "something";
    double score = matcher.matchScore(s1, s2);
    assertEquals(0.0, score, 0.001, "Empty string should result in 0.0 score");
  }
}
