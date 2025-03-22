package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.textutil.restoration.Sentence;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextSentenceMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TextSentenceMatcherTest {
  @Test
  public void testSingleInput() {
    String text1 = "Hello, this is a sample text. This is a sample text! How are you?";
    Sentence[] result = new TextSentenceMatcher().getSentences(text1);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[0]);
    Assertions.assertEquals("This is a sample text!", result[1].texts()[0]);
    Assertions.assertEquals("How are you?", result[2].texts()[0]);
  }

  @Test
  public void testSingleInput2() {
    String text1 = """
        Yard by yard, life is hard. Inch by inch, life's a cinch.
        Ladybug, ladybug, fly away home. The cows are in the meadow. The sheep are in the corn.
        Where is the little logbook? The birds are in the sky. The fish are in the sea.
        """;
    Sentence[] result = new TextSentenceMatcher().getSentences(text1);
    Assertions.assertEquals("Yard by yard, life is hard.", result[0].texts()[0]);
    Assertions.assertEquals("Inch by inch, life's a cinch.\n", result[1].texts()[0]);
    Assertions.assertEquals("Ladybug, ladybug, fly away home.", result[2].texts()[0]);
    Assertions.assertEquals("The cows are in the meadow.", result[3].texts()[0]);
    Assertions.assertEquals("The sheep are in the corn.\n", result[4].texts()[0]);
    Assertions.assertEquals("Where is the little logbook?", result[5].texts()[0]);
    Assertions.assertEquals("The birds are in the sky.", result[6].texts()[0]);
    Assertions.assertEquals("The fish are in the sea.\n", result[7].texts()[0]);
  }

  @Test
  public void testSingleInput3() {
    String text1 = """
        DOG CAT BIRD
        1st of August. 1941.
        FISH HORSE MOUSE
        BEAR WOLF FOX
        """;
    Sentence[] result = new TextSentenceMatcher().getSentences(text1);
    Assertions.assertEquals("DOG CAT BIRD\n1st of August.", result[0].texts()[0]);
    Assertions.assertEquals("1941.\n", result[1].texts()[0]);
    Assertions.assertEquals("FISH HORSE MOUSE\nBEAR WOLF FOX\n", result[2].texts()[0]);
  }

  @Test
  public void testBasic() {
    TextSentenceMatcher textSentenceMatcher = new TextSentenceMatcher();
    String text1 = "Hello, this is a sample text. This is a sample text! How are you?";
    String text2 = "Hello, this is a sample text. This is a sample text! How are you?";
    String text3 = "Hello, this is a sample text. This is sample text! How are ywu?";
    Sentence[] result = textSentenceMatcher.getSentences(text1, text2, text3);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[0]);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[1]);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[2]);
    Assertions.assertEquals("This is a sample text!", result[1].texts()[0]);
    Assertions.assertEquals("This is a sample text!", result[1].texts()[1]);
    Assertions.assertEquals("This is sample text!", result[1].texts()[2]);
    Assertions.assertEquals("How are you?", result[2].texts()[0]);
    Assertions.assertEquals("How are you?", result[2].texts()[1]);
    Assertions.assertEquals("How are ywu?", result[2].texts()[2]);
  }

  @Test
  public void testMedium() {
    TextSentenceMatcher textSentenceMatcher = new TextSentenceMatcher();
    String text1 = "Hello, this is a sample text. This is a sample text! How are you?";
    String text2 = "Hello, this is a sample text! This is a sample text! How are you?";
    String text3 = "Hello, this is a sample text. This is sample text, How are ywu";
    Sentence[] result = textSentenceMatcher.getSentences(text1, text2, text3);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[0]);
    Assertions.assertEquals("Hello, this is a sample text!", result[0].texts()[1]);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[2]);
    Assertions.assertEquals("This is a sample text!", result[1].texts()[0]);
    Assertions.assertEquals("This is a sample text!", result[1].texts()[1]);
    Assertions.assertEquals("This is sample text,", result[1].texts()[2]);
    Assertions.assertEquals("How are you?", result[2].texts()[0]);
    Assertions.assertEquals("How are you?", result[2].texts()[1]);
    Assertions.assertEquals("How are ywu", result[2].texts()[2]);
  }

  @Test
  public void testExtra() {
    TextSentenceMatcher textSentenceMatcher = new TextSentenceMatcher();
    String text1 = "Hello, this is a sample text. WHAT!!!! This is a sample text! How are you?";
    String text2 = "Hello, this is a sample text! This is a sample text! How are you?";
    String text3 = "Hello, this is a sample text. This is sample text, How are ywu";
    Sentence[] result = textSentenceMatcher.getSentences(text1, text2, text3);
    Assertions.assertEquals("Hello, this is a sample text. WHAT!!!!", result[0].texts()[0]);
    Assertions.assertEquals("Hello, this is a sample text!", result[0].texts()[1]);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[2]);
    Assertions.assertEquals("This is a sample text!", result[1].texts()[0]);
    Assertions.assertEquals("This is a sample text!", result[1].texts()[1]);
    Assertions.assertEquals("This is sample text,", result[1].texts()[2]);
    Assertions.assertEquals("How are you?", result[2].texts()[0]);
    Assertions.assertEquals("How are you?", result[2].texts()[1]);
    Assertions.assertEquals("How are ywu", result[2].texts()[2]);
  }

  @Test
  public void testMissing() {
    TextSentenceMatcher textSentenceMatcher = new TextSentenceMatcher();
    String text1 = "Hello, this is a sample text. This is a sample text! How are you?";
    String text2 = "Hello, this is a sample text! This is a sample text! How are you?";
    String text3 = "Hello, this is a sample text. This is sample text,";
    Sentence[] result = textSentenceMatcher.getSentences(text1, text2, text3);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[0]);
    Assertions.assertEquals("Hello, this is a sample text!", result[0].texts()[1]);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[2]);
    Assertions.assertEquals("This is a sample text!", result[1].texts()[0]);
    Assertions.assertEquals("This is a sample text!", result[1].texts()[1]);
    Assertions.assertEquals("This is sample text,", result[1].texts()[2]);
    Assertions.assertEquals("How are you?", result[2].texts()[0]);
    Assertions.assertEquals("How are you?", result[2].texts()[1]);
    Assertions.assertEquals("", result[2].texts()[2]);
  }

  @Test
  public void testMissingInMiddle() {
    TextSentenceMatcher textSentenceMatcher = new TextSentenceMatcher();
    String text1 = "Hello, this is a sample text. This is a sample text! How are you?";
    String text2 = "Hello, this is a sample text! How are you?";
    String text3 = "Hello, this is a sample text. This is sample text, How are ywu?";
    Sentence[] result = textSentenceMatcher.getSentences(text1, text2, text3);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[0]);
    Assertions.assertEquals("Hello, this is a sample text!", result[0].texts()[1]);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[2]);
    Assertions.assertEquals("This is a sample text!", result[1].texts()[0]);
    Assertions.assertEquals("", result[1].texts()[1]);
    Assertions.assertEquals("This is sample text,", result[1].texts()[2]);
    Assertions.assertEquals("How are you?", result[2].texts()[0]);
    Assertions.assertEquals("How are you?", result[2].texts()[1]);
    Assertions.assertEquals("How are ywu?", result[2].texts()[2]);
  }

  @Test
  public void testJoinedWords() {
    TextSentenceMatcher textSentenceMatcher = new TextSentenceMatcher();
    String text1 = "Hello, this is a sample text. This is a sample text! How are you?";
    String text2 = "Hello, this is a sampletext. This is a sample text! How are you?";
    String text3 = "Hello, this is a sample text. This is asample text! How are-ywu?";
    Sentence[] result = textSentenceMatcher.getSentences(text1, text2, text3);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[0]);
    Assertions.assertEquals("Hello, this is a sampletext.", result[0].texts()[1]);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[2]);
    Assertions.assertEquals("This is a sample text!", result[1].texts()[0]);
    Assertions.assertEquals("This is a sample text!", result[1].texts()[1]);
    Assertions.assertEquals("This is asample text!", result[1].texts()[2]);
    Assertions.assertEquals("How are you?", result[2].texts()[0]);
    Assertions.assertEquals("How are you?", result[2].texts()[1]);
    Assertions.assertEquals("How are-ywu?", result[2].texts()[2]);
  }

  @Test
  public void testLineBreaksAndWhiteSpaces() {
    TextSentenceMatcher textSentenceMatcher = new TextSentenceMatcher();
    String text1 = "Hello, this is a sample text.\n This is a \nsample text! How are you?\n";
    String text2 = "Hello, \tthis is a     sampletext.\n This is\n a sample text! How are you?\n";
    String text3 = "Hello, this is a sample text. This is asample text! How are-ywu?\n";
    Sentence[] result = textSentenceMatcher.getSentences(text1, text2, text3);
    Assertions.assertEquals("Hello, this is a sample text.\n", result[0].texts()[0]);
    Assertions.assertEquals("Hello, \tthis is a     sampletext.\n", result[0].texts()[1]);
    Assertions.assertEquals("Hello, this is a sample text.", result[0].texts()[2]);
    Assertions.assertEquals("This is a \nsample text!", result[1].texts()[0]);
    Assertions.assertEquals("This is\n a sample text!", result[1].texts()[1]);
    Assertions.assertEquals("This is asample text!", result[1].texts()[2]);
    Assertions.assertEquals("How are you?\n", result[2].texts()[0]);
    Assertions.assertEquals("How are you?\n", result[2].texts()[1]);
    Assertions.assertEquals("How are-ywu?\n", result[2].texts()[2]);
  }
}
