package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.textutil.restoration.TextSentenceSplitter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TextSentenceSplitterTest {
  @Test
  public void testSplit() {
    String text1 = "Hello, this is a sample text. This is a sample text! How are you?";
    String[] result = new TextSentenceSplitter().getSentences(text1);
    Assertions.assertEquals("Hello, this is a sample text.", result[0]);
    Assertions.assertEquals("This is a sample text!", result[1]);
    Assertions.assertEquals("How are you?", result[2]);
  }

  @Test
  public void testSplit2() {
    String text1 = """
        Yard by yard, life is hard. Inch by inch, life's a cinch.
        Ladybug, ladybug, fly away home. The cows are in the meadow. The sheep are in the corn.
        Where is the little logbook? The birds are in the sky. The fish are in the sea.
        """;
    String[] result = new TextSentenceSplitter().getSentences(text1);
    Assertions.assertEquals("Yard by yard, life is hard.", result[0]);
    Assertions.assertEquals("Inch by inch, life's a cinch.\n", result[1]);
    Assertions.assertEquals("Ladybug, ladybug, fly away home.", result[2]);
    Assertions.assertEquals("The cows are in the meadow.", result[3]);
    Assertions.assertEquals("The sheep are in the corn.\n", result[4]);
    Assertions.assertEquals("Where is the little logbook?", result[5]);
    Assertions.assertEquals("The birds are in the sky.", result[6]);
    Assertions.assertEquals("The fish are in the sea.\n", result[7]);
  }

  @Test
  public void testSplit3() {
    String text1 = """
        DOG CAT BIRD
        1st of August. 1941.
        FISH HORSE MOUSE
        BEAR WOLF FOX
        """;
    String[] result = new TextSentenceSplitter().getSentences(text1);
    Assertions.assertEquals("DOG CAT BIRD\n1st of August.", result[0]);
    Assertions.assertEquals("1941.\n", result[1]);
    Assertions.assertEquals("FISH HORSE MOUSE\nBEAR WOLF FOX\n", result[2]);
  }

  @Test
  public void testSplitWithRN() {
    String text1 = "Hello, this is a sample text.\r\n\r\n This is a sample text! How are you?";
    String[] result = new TextSentenceSplitter().getSentences(text1);
    Assertions.assertEquals("Hello, this is a sample text.\r\n\r\n", result[0]);
    Assertions.assertEquals("This is a sample text!", result[1]);
    Assertions.assertEquals("How are you?", result[2]);
  }

  @Test
  public void testLineBreaksAndWhiteSpaces() {
    TextSentenceSplitter textSentenceSplitter = new TextSentenceSplitter();
    String text1 = "Hello, this is a sample text.\n This is a \nsample text! How are you?\n";
    String text2 = "Hello, \tthis is a     sampletext.\n This is\n a sample text! How are you?\n";
    String text3 = "Hello, this is a sample text. This is asample text! How are-ywu?\n\n";
    String[] result0 = textSentenceSplitter.getSentences(text1);
    String[] result1 = textSentenceSplitter.getSentences(text2);
    String[] result2 = textSentenceSplitter.getSentences(text3);

    Assertions.assertEquals("Hello, this is a sample text.\n", result0[0]);
    Assertions.assertEquals("Hello, \tthis is a     sampletext.\n", result1[0]);
    Assertions.assertEquals("Hello, this is a sample text.", result2[0]);
    Assertions.assertEquals("This is a \nsample text!", result0[1]);
    Assertions.assertEquals("This is\n a sample text!", result1[1]);
    Assertions.assertEquals("This is asample text!", result2[1]);
    Assertions.assertEquals("How are you?\n", result0[2]);
    Assertions.assertEquals("How are you?\n", result1[2]);
    Assertions.assertEquals("How are-ywu?\n\n", result2[2]);
  }

  @Test
  public void testExtra() {
    TextSentenceSplitter textSentenceSplitter = new TextSentenceSplitter();
    String text1 = "Hello, this is a sample text. WHAT!!!! This is a sample text! How are you?";
    String[] result = textSentenceSplitter.getSentences(text1);

    Assertions.assertEquals(4, result.length);
    Assertions.assertEquals("Hello, this is a sample text.", result[0]);
    Assertions.assertEquals("WHAT!!!!", result[1]);
    Assertions.assertEquals("This is a sample text!", result[2]);
    Assertions.assertEquals("How are you?", result[3]);
  }

}
