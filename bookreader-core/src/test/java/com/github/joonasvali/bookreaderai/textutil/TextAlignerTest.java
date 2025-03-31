package com.github.joonasvali.bookreaderai.textutil;

import static org.junit.jupiter.api.Assertions.*;

import com.github.joonasvali.bookreaderai.textutil.restoration.MajorityVoter;
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
    assertArrayEquals(new String[0], result.getAlignedTexts()[0]);
  }

  @Test
  public void testEmptyInput() {
    TextAligner.AlignmentResult result = aligner.alignTexts(new String[]{});
    assertFalse(result.isSuccess());
    assertArrayEquals(new String[] { }, result.getAlignedTexts()[0]);
  }

  @Test
  public void testSingleText() {
    String text = "The quick brown fox";
    TextAligner.AlignmentResult result = aligner.alignTexts(new String[]{text});

    assertTrue(result.isSuccess());
    assertArrayEquals(new String[] { "The quick brown fox" }, result.getAlignedTexts()[0]);
  }

  @Test
  public void testMultipleIdenticalTexts() {
    String text = "The quick brown fox";
    String[] texts = {text, text, text};
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertTrue(result.isSuccess());
    assertArrayEquals(new String[] { "The quick brown fox" }, result.getAlignedTexts()[0]);
    assertArrayEquals(new String[] { "The quick brown fox" }, result.getAlignedTexts()[1]);
    assertArrayEquals(new String[] { "The quick brown fox" }, result.getAlignedTexts()[2]);
    assertTrue(result.isSuccess());
  }


  @Test
  public void testTextWithLineBreak() {
    String text1 = "Thw quick brown fox\n";
    String text2 = "The quick brown fox";
    String text3 = "The quick red fox\n";
    String[] texts = {text1, text2, text3};
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertArrayEquals(new String[] { "Thw quick brown fox\n" }, result.getAlignedTexts()[0]);
    assertArrayEquals(new String[] { "The quick brown fox" }, result.getAlignedTexts()[1]);
    assertArrayEquals(new String[] { "The quick red fox\n" }, result.getAlignedTexts()[2]);
    assertTrue(result.isSuccess());
  }

  @Test
  public void testExtraSentences() {
    String text1 = "How are you?\nI'm xyz. what. good.\nWhat are you doing?";
    String text2 = "How are you?\nI'm good.\nWhat are you doing?";
    String text3 = "How are you?\nI'm good.\nWhat re you doing?";

    String[] texts = {text1, text2, text3};
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertArrayEquals(new String[] { "How are you?\n", "good.\n", "What are you doing?" }, result.getAlignedTexts()[0]);
    assertArrayEquals(new String[] { "How are you?\n", "I'm good.\n", "What are you doing?" }, result.getAlignedTexts()[1]);
    assertArrayEquals(new String[] { "How are you?\n", "I'm good.\n", "What re you doing?" }, result.getAlignedTexts()[2]);
    assertTrue(result.isSuccess());
  }

  @Test
  public void testExtraSentence() {
    String text1 = "No way. Hello, this is a sample text. This is a sample text! How are you? I'm god. How r you?";
    String text2 = "Hello, this is a. This is a-sample text! How are you? I'm good. How are you?";
    String text3 = "Hello, this is a sample text. This is a sample text. How are you? I'm good. How are you?";

    String[] texts = {text1, text2, text3};
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertArrayEquals(new String[] { "Hello, this is a sample text.", "This is a sample text!", "How are you?", "I'm god.", "How r you?" }, result.getAlignedTexts()[0]);
    assertArrayEquals(new String[] { "Hello, this is a.", "This is a-sample text!", "How are you?", "I'm good.", "How are you?" }, result.getAlignedTexts()[1]);
    assertArrayEquals(new String[] { "Hello, this is a sample text.", "This is a sample text.", "How are you?", "I'm good.", "How are you?" }, result.getAlignedTexts()[2]);
    assertTrue(result.isSuccess());
  }

  @Test
  public void testMissingSentence() {
    String text1 = "This is a sample text! How are you? I'm god. How r you?";
    String text2 = "Hello, this is a sample text. This is a-sample text! How are you? I'm good. How are you?";
    String text3 = "Hello, this is a sample text. This is a sample text. How are you? I'm good. How are you?";

    String[] texts = {text1, text2, text3};
    TextAligner.AlignmentResult result = aligner.alignTexts(texts);
    assertArrayEquals(new String[] { "", "This is a sample text!", "How are you?", "I'm god.", "How r you?" }, result.getAlignedTexts()[0]);
    assertArrayEquals(new String[] { "Hello, this is a sample text.", "This is a-sample text!", "How are you?", "I'm good.", "How are you?" }, result.getAlignedTexts()[1]);
    assertArrayEquals(new String[] { "Hello, this is a sample text.", "This is a sample text.", "How are you?", "I'm good.", "How are you?" }, result.getAlignedTexts()[2]);
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

    assertArrayEquals(new String[] { "xyz.\n", "\n14.", "august 1941.", "a.\n", "Mice and men.", "Bread and butter.", "Cats and dogs.\n" }, result.getAlignedTexts()[0]);
    assertArrayEquals(new String[] { "xyz.\n", "\n14.", "august 1941.", "a.\n", "\nMice and men.\n", "Bread and butter.", "Cats\nand dogs.\n" }, result.getAlignedTexts()[1]);
    assertArrayEquals(new String[] { "xyz.\n", "\n14.", "august 1941.", "a.\n", "\nMice and men.\n", "Bread and butter.", "Cats and dogs\n" }, result.getAlignedTexts()[2]);
  }

}