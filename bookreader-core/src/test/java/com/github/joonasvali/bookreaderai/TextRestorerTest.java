package com.github.joonasvali.bookreaderai;

import com.github.joonasvali.bookreaderai.textutil.restoration.TextRestorer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TextRestorerTest {
  @Test
  public void test() {
    String text1 = "Hello, this is a sample text. This is a sample text! How are you? I'm god. How r you?";
    String text2 = "Hello, this is a. This is a-sample text! How are you? I'm good. How are you?";
    String text3 = "Hello, this is a sample text. This is a sample text. How are you? I'm good. How are you?";
    String text4 = "garbage";

    TextRestorer textRestorer = new TextRestorer();
    String result = textRestorer.restoreText(text1, text2, text3, text4);
    Assertions.assertEquals("Hello, this is a sample text. This is a sample text. How are you? I'm good. How are you?", result);
  }

  @Test
  public void testExtraWords() {
    String text1 = "No way. Hello, this is a sample text. This is a sample text! How are you? I'm god. How r you?";
    String text2 = "Hello, this is a. This is a-sample text! How are you? I'm good. How are you?";
    String text3 = "Hello, this is a sample text. This is a sample text. How are you? I'm good. How are you?";
    String text4 = "garbage";

    TextRestorer textRestorer = new TextRestorer();
    String result = textRestorer.restoreText(text1, text2, text3, text4);
    Assertions.assertEquals("Hello, this is a sample text. This is a sample text. How are you? I'm good. How are you?", result);
  }

  @Test
  public void testLineBreaks() {
    String text1 = "Hello, this is a sample text.\n This is a sample text! How are you? I'm god. How r you?";
    String text2 = "Hello, this is a. This is a-sample text! How are you? I'm good. How are you?";
    String text3 = "Hello, this is a sample text.\n This is a sample text. How are you? I'm good. How are you?";

    TextRestorer textRestorer = new TextRestorer();
    String result = textRestorer.restoreText(text1, text2, text3);
    Assertions.assertEquals("Hello, this is a sample text.\nThis is a sample text. How are you? I'm good. How are you?", result);
  }

  @Test
  public void testLineBreaksNoSpace() {
    String text1 = "Hello, this is a sample text.\nThis is a sample text! How are you? I'm god. How r you?";
    String text2 = "Hello, this is a. This is a-sample text! How are you? I'm good. How are you?";
    String text3 = "Hello, this is a sample text.\nThis is a sample text. How are you? I'm good. How are you?";

    TextRestorer textRestorer = new TextRestorer();
    String result = textRestorer.restoreText(text1, text2, text3);
    Assertions.assertEquals("Hello, this is a sample text.\nThis is a sample text. How are you? I'm good. How are you?", result);
  }
}
