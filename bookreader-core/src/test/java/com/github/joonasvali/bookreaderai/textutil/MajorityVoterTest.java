package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.textutil.restoration.MajorityVoter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MajorityVoterTest {

  private final MajorityVoter voter = new MajorityVoter();

  @Test
  public void testNullInput() {
    MajorityVoter.VoteResult result = voter.vote(null);
    assertFalse(result.isSuccess());
    assertEquals("", result.getResultingText());
  }

  @Test
  public void testEmptyInput() {
    MajorityVoter.VoteResult result = voter.vote(new String[]{});
    assertFalse(result.isSuccess());
    assertEquals("", result.getResultingText());
  }

  @Test
  public void testSingleText() {
    String text = "The quick brown fox";
    MajorityVoter.VoteResult result = voter.vote(new String[]{text});

    assertTrue(result.isSuccess());
    assertEquals("The quick brown fox", result.getResultingText());
  }

  @Test
  public void testMultipleIdenticalTexts() {
    String text = "The quick brown fox";
    String[] texts = { text, text, text };
    MajorityVoter.VoteResult result = voter.vote(texts);
    assertTrue(result.isSuccess());
    assertEquals("The quick brown fox", result.getResultingText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testMultipleTextsWithDifferences() {
    // Two texts agree on "fox", one has "fix" due to a typo.
    String text1 = "The quick brown fox";
    String text2 = "The quick brown fix";
    String text3 = "The quick brown fox";
    String[] texts = { text1, text2, text3 };
    MajorityVoter.VoteResult result = voter.vote(texts);
    // Majority vote should yield "fox" for the 4th word.
    assertEquals("The quick brown fox", result.getResultingText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testShortInput() {
    // Two texts agree on "fox", one has "fix" due to a typo.
    String text1 = "I'm god.";
    String text2 = "I'm good.";
    String text3 = "I'm good.";
    String[] texts = { text1, text2, text3 };
    MajorityVoter.VoteResult result = voter.vote(texts);
    // Majority vote should yield "fox" for the 4th word.
    assertEquals("I'm good.", result.getResultingText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testTextsWithDifferentLengths() {
    // Some texts have missing words at certain positions.
    String text1 = "The quick brown fox";
    String text2 = "The quick brown";
    String text3 = "The quick brown fox jumps";
    String[] texts = { text1, text2, text3 };
    MajorityVoter.VoteResult result = voter.vote(texts);
    assertEquals("The quick brown fox", result.getResultingText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testTextWithAlotOfMistakes() {
    // Some texts have missing words at certain positions.
    String text1 = "Thw quick brown fox";
    String text2 = "The qick brown fix";
    String text3 = "The quick red fox";
    String[] texts = { text1, text2, text3 };
    MajorityVoter.VoteResult result = voter.vote(texts);
    assertEquals("The quick brown fox", result.getResultingText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testTextWithAlotOfMistakesAndDifferentLength() {
    // Some texts have missing words at certain positions.
    String text1 = "Thw quick brown fox";
    String text2 = "The qick brown";
    String text3 = "The quick red fox";
    String[] texts = { text1, text2, text3 };
    MajorityVoter.VoteResult result = voter.vote(texts);
    assertEquals("The quick brown fox", result.getResultingText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testTextWithLineBreak() {
    // Some texts have missing words at certain positions.
    String text1 = "Thw quick brown fox\n";
    String text2 = "The quick brown fox";
    String text3 = "The quick red fox\n";
    String[] texts = { text1, text2, text3 };
    MajorityVoter.VoteResult result = voter.vote(texts);
    assertEquals("The quick brown fox\n", result.getResultingText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testTextWithLineBreakInMiddle() {
    // Some texts have missing words at certain positions.
    String text1 = "Thw quick\n brown fox";
    String text2 = "The quick brown fox";
    String text3 = "The quick\n red fox";
    String[] texts = { text1, text2, text3 };
    MajorityVoter.VoteResult result = voter.vote(texts);
    assertEquals("The quick\n brown fox", result.getResultingText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testPunctuation() {
    // Some texts have missing words at certain positions.
    String text1 = "Thw quick\n brown fox.";
    String text2 = "The quick brown fox.";
    String text3 = "The quick\n red fox.";
    String[] texts = { text1, text2, text3 };
    MajorityVoter.VoteResult result = voter.vote(texts);
    assertEquals("The quick\n brown fox.", result.getResultingText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testTextWithLineBreakAndMismatchingConnectedWord() {
    // Some texts have missing words at certain positions.
    String text1 = "Thw quick brown fix\n";
    String text2 = "The quick brown fox";
    String text3 = "The quick red fox\n";
    String[] texts = { text1, text2, text3 };
    MajorityVoter.VoteResult result = voter.vote(texts);
    assertEquals("The quick brown fox\n", result.getResultingText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testBattleStarGalactica() {
    String text1 = "bears, beets, Battlestar Galactica.";
    String text2 = "bears, bears, Battlestar Galactica";
    String text3 = "..still. bears, beets, Battle star Galactica";
    String[] texts = { text1, text2, text3 };
    MajorityVoter.VoteResult result = voter.vote(texts);
    assertEquals("bears, beets, Battlestar Galactica", result.getResultingText());
    assertTrue(result.isSuccess());
  }
}
