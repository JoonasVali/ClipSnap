package com.github.joonasvali.bookreaderai.textutil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextJoinerTest {
  @Test
  public void testTextJoinerWithPartialMatch() {
    TextJoiner textJoiner = new TextJoiner();
    String text1 = "Hello, this is a sample text";
    String text2 = "text that continues the story.";
    String expected = "Hello, this is a sample text that continues the story.";
    String result = textJoiner.join(text1, text2);
    assertEquals(expected, result);
  }

  @Test
  public void testTextJoinerWithPartialMatchIgnoringCaseAndWhitespace() {
    TextJoiner textJoiner = new TextJoiner();
    String text1 = "Hello, this is a sample text ";
    String text2 = " Text that continues the story.";
    String expected = "Hello, this is a sample text that continues the story.";
    String result = textJoiner.join(text1, text2);
    assertEquals(expected, result);
  }


  @Test
  public void testTextJoinerWithSplitWordIgnoringCase() {
    TextJoiner textJoiner = new TextJoiner();
    String text1 = "The quick brown fox JUM";
    String text2 = "mps over the lazy dog.";
    String expected = "The quick brown fox JUMps over the lazy dog.";
    String result = textJoiner.join(text1, text2);
    assertEquals(expected, result);
  }

  @Test
  public void testTextJoinerWithSplitWordAndWhitespace() {
    TextJoiner textJoiner = new TextJoiner();
    String text1 = "The quick brown fox jum ";
    String text2 = " mps over the lazy dog.";
    String expected = "The quick brown fox jumps over the lazy dog.";
    String result = textJoiner.join(text1, text2);
    assertEquals(expected, result);
  }

  @Test
  public void testTextJoinerWithMultipleSplits() {
    TextJoiner textJoiner = new TextJoiner();
    String text1 = "A journey of a thous and miles begins with";
    String text2 = "thousand miles begins with a single step.";
    String expected = "A journey of a thous and miles begins with a single step.";
    String result = textJoiner.join(text1, text2);
    assertEquals(expected, result);
  }

  @Test
  public void testTextJoinerWithMultipleLines() {
    TextJoiner textJoiner = new TextJoiner();
    String text1 = """
      A journey of a thousand miles begins with
      a single step. With a little bit of luck
      and a lot of hard work, you can achieve
      anything you set your mind to. And this
    """;
    String text2 = """
      Anything you set Your mind to.
      And this is the beginning of a new era.
      The era of the unstoppable force that
      will change the world forever.
    """;
    String expected = """
    A journey of a thousand miles begins with
      a single step. With a little bit of luck
      and a lot of hard work, you can achieve
      anything you set your mind to. And this is the beginning of a new era.
      The era of the unstoppable force that
      will change the world forever.""";
    String result = textJoiner.join(text1, text2);
    assertEquals(expected, result);
  }

  @Test
  public void testTextJoinerWithMultipleSplitsIgnoringCaseAndWhitespace() {
    TextJoiner textJoiner = new TextJoiner();
    String text1 = "A journey of a thousand miles";
    String text2 = " AND miles begins with a single step.";
    String expected = "A journey of a thousand miles begins with a single step.";
    String result = textJoiner.join(text1, text2);
    assertEquals(expected, result);
  }

  @Test
  public void testTextJoinerNotHavingFalsePositives() {
    TextJoiner textJoiner = new TextJoiner();
    String text1 = "Cakes that are delicious and healthy";
    String text2 = "are delicious and bad for your health";
    String expected = "Cakes that are delicious and healthy are delicious and bad for your health";
    String result = textJoiner.join(text1, text2);
    assertEquals(expected, result);
  }
}
