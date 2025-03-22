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
    String text1 = "The quick brown fox jumps";
    String text2 = "jum PS over the lazy dog.";
    String expected = "The quick brown fox jumps over the lazy dog.";
    String result = textJoiner.join(text1, text2);
    assertEquals(expected, result);
  }

  @Test
  public void testTextJoinerWithSplitWordAndWhitespace() {
    TextJoiner textJoiner = new TextJoiner();
    String text1 = "Let it be known, that the quick brown fox jumps";
    String text2 = "brownfox jumps over the lazy dog.";
    String expected = "Let it be known, that the quick brown fox jumps over the lazy dog.";
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
  public void testTextJoinerWithMultipleSplitsWithPunctuationDifference() {
    TextJoiner textJoiner = new TextJoiner();
    String text1 = "A journey of a thous and miles, begins with";
    String text2 = "thousand miles. Begins with a single step.";
    String expected = "A journey of a thous and miles, begins with a single step.";
    String result = textJoiner.join(text1, text2);
    assertEquals(expected, result);
  }

  @Test
  public void testTextJoinerWithMultipleSplitsWithPunctuationMissingFromFirst() {
    TextJoiner textJoiner = new TextJoiner();
    String text1 = "A journey of a thous and miles begins with";
    String text2 = "thousand miles. begins with a single step,";
    String expected = "A journey of a thous and miles begins with a single step,";
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
        will change the world forever.
        """;
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
  public void testTextJoinerFalsePositives() {
    // This is unfortunately a false positive, but expected from fuzzy matching.
    TextJoiner textJoiner = new TextJoiner();
    String text1 = "Cakes that are delicious and healthy";
    String text2 = "are delicious and bad for your health";
    String expected = "Cakes that are delicious and bad for your health";
    String result = textJoiner.join(text1, text2);
    assertEquals(expected, result);
  }

  @Test
  public void testJoinStoryWithTranscriptionErrors() {
    TextJoiner joiner = new TextJoiner();
    String text1 = "Max chased butterflies, but a black cat named Luna teased him by darting up trees. He tried to climb. the had fub";
    String text2 = "threes. he tried to climb. They had fun and became inseparable. Max loved that day";
    String expected = "Max chased butterflies, but a black cat named Luna teased him by darting up trees. He tried to climb. They had fun and became inseparable. Max loved that day";

    assertEquals(expected, joiner.join(text1, text2));
  }

  @Test
  public void testJoinStoryWithTranscriptionErrorsAndCasingDifference() {
    TextJoiner joiner = new TextJoiner();
    String text1 = "MAX chased butterflies, but a black cat named Luna teased him by darting up trees. He tried to climb. THEY had fub";
    String text2 = "threes. he tried TO climb. They had fun and BECAME inseparable. Max loved that day";
    String expected = "MAX chased butterflies, but a black cat named Luna teased him by darting up trees. He tried to climb. They had fun and BECAME inseparable. Max loved that day";

    assertEquals(expected, joiner.join(text1, text2));
  }


  @Test
  public void testJoinStoryWithNoMatch() {
    TextJoiner joiner = new TextJoiner();
    String text1 = "MAX chased butterflies, but a black cat named Luna teased him by darting up trees.";
    String text2 = "He tried TO climb. They had fun and BECAME inseparable. Max loved that day";
    String expected = "MAX chased butterflies, but a black cat named Luna teased him by darting up trees. He tried TO climb. They had fun and BECAME inseparable. Max loved that day";

    assertEquals(expected, joiner.join(text1, text2));
  }


  @Test
  public void testJoiningTextWithRepeats() {
    TextJoiner joiner = new TextJoiner();
    String text1 = """
        Ladybug, ladybug, fly away home.
        The cows are in the meadow. The sheep are in the corn.
        Where is the little logbook? The birds are in the sky. The fish are in the sea.
        The bees are in the hive. The ants are in the ground.
        Yard by yard, life is hard. Inch by inch, life's a cinch. kdoakaeo
        """;
    String text2 = """
        Yard by yard, life is hard. Inch by inch, life's a cinch.
        Ladybug, ladybug, fly away home. The cows are in the meadow. The sheep are in the corn.
        Where is the little logbook? The birds are in the sky. The fish are in the sea.
        """;
    String expected = """
        Ladybug, ladybug, fly away home.
        The cows are in the meadow. The sheep are in the corn.
        Where is the little logbook? The birds are in the sky. The fish are in the sea.
        The bees are in the hive. The ants are in the ground.
        Yard by yard, life is hard. Inch by inch, life's a cinch.
        Ladybug, ladybug, fly away home. The cows are in the meadow. The sheep are in the corn.
        Where is the little logbook? The birds are in the sky. The fish are in the sea.
        """;
    assertEquals(expected, joiner.join(text1, text2));
  }

  @Test
  public void testJoiningTextWithPartialMatchWhileThereIsFullMatchBeforeThat() {
    TextJoiner joiner = new TextJoiner();
    String text1 = """
        31st of July. 1941.
        apple banana cherry.
        DOG CAT BIR…
        """;

    String text2 = """
        DOG CAT BIRD
        1st of August. 1941.
        FISH HORSE MOUSE
        BEAR WOLF FOX
        """;

    // 1941. is not expected to be joined. DOG CAT BIR… and DOG CAT BIRD are expected to be joined.
    String expected = """
        31st of July. 1941.
        apple banana cherry.
        DOG CAT BIRD
        1st of August. 1941.
        FISH HORSE MOUSE
        BEAR WOLF FOX
        """;
    assertEquals(expected, joiner.join(text1, text2));
  }

  @Test
  public void testDoubleMatch() {
    TextJoiner joiner = new TextJoiner();
    String text1 = """
        Ladybug, ladybug, fly away home.
        Whistle while you work.
        red blue green.
        PINK PURPLE ORANGE.
        Whistle while you wo
        """;

    String text2 = """
        Whistle while you work.
        Danger is my middle name.
        FISH HORSE MOUSE
        """;

    String expected = """
        Ladybug, ladybug, fly away home.
        Whistle while you work.
        red blue green.
        PINK PURPLE ORANGE.
        Whistle while you work.
        Danger is my middle name.
        FISH HORSE MOUSE
        """;
    assertEquals(expected, joiner.join(text1, text2));
  }


  @Test
  public void testDoubleMatchWithSentenceHavingExtraWordsOnPreviousLine() {
    TextJoiner joiner = new TextJoiner();
    String text1 = """
        Ladybug, ladybug, fly away home.
        Whistle while you work.
        red blue green.
        PINK PURPLE ORANGE
        Whistle while you wo...
        """;

    String text2 = """
        Whistle while you work.
        Danger is my middle name.
        FISH HORSE MOUSE
        """;

    // 1941. is not expected to be joined. DOG CAT BIR… and DOG CAT BIRD are expected to be joined.
    String expected = """
        Ladybug, ladybug, fly away home.
        Whistle while you work.
        red blue green.
        PINK PURPLE ORANGE
        Whistle while you work.
        Danger is my middle name.
        FISH HORSE MOUSE
        """;
    assertEquals(expected, joiner.join(text1, text2));
  }
}