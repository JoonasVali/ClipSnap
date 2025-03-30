package com.github.joonasvali.bookreaderai.textutil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LineUtilTest {
  @Test
  public void testLineBreaker() {
    LineUtil lineUtil = new LineUtil();
    String text = "Hello, this is a sample text that continues the story.";
    String expected = "Hello, this is\na sample text\nthat continues\nthe story.";
    String result = lineUtil.lineBreakAfterEvery(text, 15);
    assertEquals(expected, result);
  }

  @Test
  public void testLineBreakerWithExistingBreaks() {
    LineUtil lineUtil = new LineUtil();
    String text = "abc\n\nHello, this is a sample text that continues the story.\n\nHello world";
    String expected = "abc\n\nHello, this is\na sample text\nthat continues\nthe story.\n\nHello world";
    String result = lineUtil.lineBreakAfterEvery(text, 15);
    assertEquals(expected, result);
  }
}