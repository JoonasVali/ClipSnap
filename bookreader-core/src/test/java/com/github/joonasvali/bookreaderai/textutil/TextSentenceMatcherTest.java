package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.textutil.restoration.Sentence;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextSentenceMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TextSentenceMatcherTest {
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
}
