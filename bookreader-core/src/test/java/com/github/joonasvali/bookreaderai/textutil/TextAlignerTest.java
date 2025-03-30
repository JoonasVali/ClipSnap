package com.github.joonasvali.bookreaderai.textutil;

import static org.junit.jupiter.api.Assertions.*;

import com.github.joonasvali.bookreaderai.textutil.restoration.TextAligner;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextRestorer;
import org.junit.jupiter.api.Assertions;
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
    String[] texts = {text, text, text};
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertTrue(result.isSuccess());
    assertEquals("The quick brown fox", result.getAlignedText());
    assertTrue(result.isSuccess());
  }


  @Test
  public void testTextWithLineBreak() {
    // Some texts have missing words at certain positions.
    String text1 = "Thw quick brown fox\n";
    String text2 = "The quick brown fox";
    String text3 = "The quick red fox\n";
    String[] texts = {text1, text2, text3};
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertEquals("The quick brown fox\n", result.getAlignedText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testExtraSentences() {
    String text1 = "How are you?\nI'm xyz. what. good.\nWhat are you doing?";
    String text2 = "How are you?\nI'm good.\nWhat are you doing?";
    String text3 = "How are you?\nI'm good.\nWhat are you doing?";

    String[] texts = {text1, text2, text3};
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertEquals("How are you?\nI'm good.\nWhat are you doing?", result.getAlignedText());
    assertTrue(result.isSuccess());
  }

  @Test
  public void testExtraWords() {
    String text1 = "No way. Hello, this is a sample text. This is a sample text! How are you? I'm god. How r you?";
    String text2 = "Hello, this is a. This is a-sample text! How are you? I'm good. How are you?";
    String text3 = "Hello, this is a sample text. This is a sample text. How are you? I'm good. How are you?";

    String[] texts = {text1, text2, text3};
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertEquals("Hello, this is a sample text. This is a sample text! How are you? I'm good. How are you?", result.getAlignedText());
    assertTrue(result.isSuccess());
  }


  @Test
  public void testExtraDotsInMiddle() {
    String text1 = """
        xyz.
        
        14. august 1941.a.
        
        ...
        Mice and men. Bread and butter. Cats and dogs.
        """;
    String text2 = """        
        xyz.
        
        14. august 1941.a.
        
        Mice and men.
        Bread and butter. Cats
        and dogs.
        """;

    String text3 = """
        xyz.
        
        14. august 1941. a.
        
        Mice and men.
        Bread and butter. Cats and dogs
        """;


    String[] texts = {text1, text2, text3};
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    Assertions.assertEquals("""
        xyz.
        
        14. august 1941. a.
        
        Mice and men. Bread and butter. Bread and butter. Cats and dogs.
        """, result.getAlignedText());
  }

}