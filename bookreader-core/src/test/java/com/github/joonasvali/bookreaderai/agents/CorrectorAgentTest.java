package com.github.joonasvali.bookreaderai.agents;

import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class CorrectorAgentTest {

  String sampleText = """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
      dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
      ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore
      eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia
      deserunt mollit anim id est laborum. Sed ut perspiciatis unde omnis iste natus error sit voluptatem
      accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et
      quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit
      aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi
      nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit,
      sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.
      Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid
      ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil
      molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?
      """;

  @Test
  public void testFixLongText() {
    CorrectorAgent agent = createMockedAgent("", "", new String[]{
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et\n" +
            "dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip\n" +
            "ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore\n" +
            "eu fugiat nulla pariatur.",
        "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia\n" +
            "deserunt mollit anim id est laborum. Sed ut perspiciatis unde omnis iste natus error sit voluptatem\n" +
            "accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et\n" +
            "quasi architecto beatae vitae dicta sunt explicabo.",
        "Nemo enim ipsam voluptatem quia voluptas sit\n" +
            "aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi\n" +
            "nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit,\n" +
            "sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.\n",
        "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid\n" +
            "ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil\n" +
            "molestiae consequat(1)ur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?\n"
    }, new String[]{
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et\n" +
            "dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip\n" +
            "ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore\n" +
            "eu fugiat nulla pariatur.",
        "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia\n" +
            "deserunt mollit anim id est laborum. Sed ut perspiciatis unde omnis iste natus error sit voluptatem\n" +
            "accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et\n" +
            "quasi architecto beatae vitae dicta sunt explicabo.",
        "Nemo enim ipsam voluptatem quia voluptas sit\n" +
            "aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi\n" +
            "nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit,\n" +
            "sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.\n",
        "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid\n" +
            "ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil\n" +
            "molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?\n"
    });
    Assertions.assertEquals(sampleText, agent.correct("""
        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et
        dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip
        ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore
        eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia
        deserunt mollit anim id est laborum. Sed ut perspiciatis unde omnis iste natus error sit voluptatem
        accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et
        quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit
        aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi
        nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit,
        sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.
        Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid
        ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil
        molestiae consequat(1)ur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?
        """).content());
  }

  @Test
  public void testFixShortText() {

    CorrectorAgent agent = createMockedAgent("", "", new String[]{
        "AB1C Lorem Ipsum",
    }, new String[]{
        "ABC Lorem Ipsum",
    });

    Assertions.assertEquals("ABC Lorem Ipsum", agent.correct("AB1C Lorem Ipsum").content());

  }

  @Test
  public void testFixShortTextLineBreak() {

    CorrectorAgent agent = createMockedAgent("", "", new String[]{
        "AB1C Lorem Ipsum\n",
    }, new String[]{
        "ABC Lorem Ipsum\n",
    });

    Assertions.assertEquals("ABC Lorem Ipsum\n", agent.correct("AB1C Lorem Ipsum\n").content());

  }

  @Test
  public void testSplitSentenceOverThousandCharacters() {
    // Build a 1003-character string: 1000 'A' + 2 'A' + newline = 1003 total
    String chunk1 = repeatChar('A', 1000);
    String chunk2 = "AA\n"; // 3 characters (2 'A' + newline)
    String input = chunk1 + chunk2; // 1003 total

    // We'll expect two calls in invoke(...):
    //   1) the first 1000 characters
    //   2) the last 3 characters
    String[] expectedCalls = {chunk1, chunk2};

    String[] mockedAnswers = {"CHUNK1_FIXED", "CHUNK2_FIXED\n"};


    CorrectorAgent agent = createMockedAgent("", "", expectedCalls, mockedAnswers);


    ProcessingResult<String> result = agent.correct(input);

    String expectedFinal = "CHUNK1_FIXEDCHUNK2_FIXED\n";
    Assertions.assertEquals(expectedFinal, result.content());
  }

  @Test
  public void testSplitSentenceOverThousandCharactersNewLines() {
    // Build a 1003-character string: 1000 'A' + 2 'A' + newline = 1003 total
    String chunk1 = repeatChar('A', 1000);
    String chunk2 = "AA\n"; // 3 characters (2 'A' + newline)
    String input = chunk1 + chunk2; // 1003 total

    // We'll expect two calls in invoke(...):
    //   1) the first 1000 characters
    //   2) the last 3 characters
    String[] expectedCalls = {chunk1, chunk2};

    String[] mockedAnswers = {"CHUNK1_FIXED.\n", "CHUNK2_FIXED\n"};


    CorrectorAgent agent = createMockedAgent("", "", expectedCalls, mockedAnswers);


    ProcessingResult<String> result = agent.correct(input);

    String expectedFinal = "CHUNK1_FIXED.\nCHUNK2_FIXED\n";
    Assertions.assertEquals(expectedFinal, result.content());
  }

  /**

   Utility method to repeat a character n times into a String. */
  private String repeatChar(char c, int n) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < n; i++) {
      sb.append(c);
    }
    return sb.toString();
  }

  private CorrectorAgent createMockedAgent(String language, String story, String[] expectations, String[] answers) {
    if (expectations.length != answers.length) {
      throw new IllegalArgumentException("Expectations and answers must have the same length.");
    }

    AtomicInteger counter = new AtomicInteger(0);
    return new CorrectorAgent(language, story) {
      @Override
      public ProcessingResult<String> invoke(String text) {
        Assertions.assertEquals(expectations[counter.get()], text);
        Assertions.assertTrue(counter.get() < expectations.length);
        String expected = expectations[counter.getAndIncrement()];
        Assertions.assertEquals(expected, text);
        return new ProcessingResult<>(answers[counter.get() - 1], 0, 0, 0);
      }
    };
  }
}
