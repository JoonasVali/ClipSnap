package com.github.joonasvali.bookreaderai.textutil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StringSelectorTest {

  @Test
  public void testVerifySimpleCase() {
    String[] texts = {"AA A", "BBB", "AA A"};
    StringSelector stringSelector = new StringSelector();
    String selected = stringSelector.getMostSimilar(texts);
    String expectedOutput = "AA A";
    assertEquals(expectedOutput, selected);
  }

  @Test
  public void testVerifyDifferentWhitespaces() {
    // Variations in whitespace should normalize to the same string.
    String[] texts = {"AA A", "AA    A", "AA\nA", "BBB"};
    StringSelector stringSelector = new StringSelector();
    String selected = stringSelector.getMostSimilar(texts);
    String expectedOutput = "AA A";
    assertEquals(expectedOutput, selected);
  }

  @Test
  public void testVerifyTieCase() {
    // When two normalized texts have the same frequency, the first occurrence should be chosen.
    String[] texts = {"BBB", "AA A", "BBB", "AA A"};
    StringSelector stringSelector = new StringSelector();
    String selected = stringSelector.getMostSimilar(texts);
    String expectedOutput = "BBB";
    assertEquals(expectedOutput, selected);
  }

  @Test
  public void testVerifySingleElement() {
    // If there's only one element, it should be selected.
    String[] texts = {"Test"};
    StringSelector stringSelector = new StringSelector();
    String selected = stringSelector.getMostSimilar(texts);
    String expectedOutput = "Test";
    assertEquals(expectedOutput, selected);
  }

  @Test
  public void testVerifyAllInputsDifferent() {
    String[] texts = {"Alpha", "Beta", "Gamma"};
    StringSelector stringSelector = new StringSelector();
    String selected = stringSelector.getMostSimilar(texts);
    assertNull(selected);
  }
}