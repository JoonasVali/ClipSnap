package com.github.joonasvali.bookreaderai.textutil;

import static org.junit.jupiter.api.Assertions.*;

import com.github.joonasvali.bookreaderai.textutil.restoration.TextAligner;
import org.junit.jupiter.api.Test;

public class TextAlignerTest {

  private final TextAligner aligner = new TextAligner();

  @Test
  public void testNullInput() {
    TextAligner.AlignmentResult result = aligner.alignTexts(null);
    assertFalse(result.isSuccess());
    assertEquals("", result.getAlignedText());
  }

  @Test
  public void testEmptyInput() {
    TextAligner.AlignmentResult result = aligner.alignTexts(new String[]{});
    assertFalse(result.isSuccess());
    assertEquals("", result.getAlignedText());
  }

  @Test
  public void testSingleText() {
    String text = "The quick brown fox";
    TextAligner.AlignmentResult result = aligner.alignTexts(new String[]{text});

    assertTrue(result.isSuccess());
    assertEquals("The quick brown fox", result.getAlignedText());
  }

  @Test
  public void testMultipleIdenticalTexts() {
    String text = "The quick brown fox";
    String[] texts = { text, text, text };
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertTrue(result.isSuccess());
    assertEquals("The quick brown fox", result.getAlignedText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testMultipleTextsWithDifferences() {
    // Two texts agree on "fox", one has "fix" due to a typo.
    String text1 = "The quick brown fox";
    String text2 = "The quick brown fix";
    String text3 = "The quick brown fox";
    String[] texts = { text1, text2, text3 };
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    // Majority vote should yield "fox" for the 4th word.
    assertEquals("The quick brown fox", result.getAlignedText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testTextsWithDifferentLengths() {
    // Some texts have missing words at certain positions.
    String text1 = "The quick brown fox";
    String text2 = "The quick brown";
    String text3 = "The quick brown fox jumps";
    String[] texts = { text1, text2, text3 };
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    // Expected breakdown:
    // Index 0: "the" from all texts.
    // Index 1: "quick" from all texts.
    // Index 2: "brown" from all texts.
    // Index 3: "fox" from text1 and text3.
    // Index 4: "jumps" from text3.+
    assertEquals("The quick brown fox jumps", result.getAlignedText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testTextWithAlotOfMistakes() {
    // Some texts have missing words at certain positions.
    String text1 = "Thw quick brown fox";
    String text2 = "The qick brown fix";
    String text3 = "The quick red fox";
    String[] texts = { text1, text2, text3 };
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertEquals("The quick brown fox", result.getAlignedText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testTextWithAlotOfMistakesAndDifferentLength() {
    // Some texts have missing words at certain positions.
    String text1 = "Thw quick brown fox";
    String text2 = "The qick brown";
    String text3 = "The quick red fox";
    String[] texts = { text1, text2, text3 };
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertEquals("The quick brown fox", result.getAlignedText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testTextWithLineBreak() {
    // Some texts have missing words at certain positions.
    String text1 = "Thw quick brown fox\n";
    String text2 = "The quick brown fox";
    String text3 = "The quick red fox\n";
    String[] texts = { text1, text2, text3 };
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertEquals("The quick brown fox\n", result.getAlignedText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testTextWithLineBreakInMiddle() {
    // Some texts have missing words at certain positions.
    String text1 = "Thw quick\n brown fox";
    String text2 = "The quick brown fox";
    String text3 = "The quick\n red fox";
    String[] texts = { text1, text2, text3 };
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertEquals("The quick\n brown fox", result.getAlignedText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testTextWithLineBreakAndMismatchingConnectedWord() {
    // Some texts have missing words at certain positions.
    String text1 = "Thw quick brown fix\n";
    String text2 = "The quick brown fox";
    String text3 = "The quick red fox\n";
    String[] texts = { text1, text2, text3 };
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertEquals("The quick brown fox\n", result.getAlignedText());
    assertTrue(result.isSuccess());
  }
}