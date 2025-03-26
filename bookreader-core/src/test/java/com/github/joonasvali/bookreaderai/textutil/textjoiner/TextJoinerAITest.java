package com.github.joonasvali.bookreaderai.textutil.textjoiner;

import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.transcribe.AgentBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.function.Predicate;

public class TextJoinerAITest {
  @Test
  public void testChooseText() {
    TextJoinerAI textJoinerAI;

    try (MockedConstruction<AgentBase> mocked = Mockito.mockConstruction(AgentBase.class, (mock, context) -> {
      Mockito.doReturn(new ProcessingResult<>("1", 0, 0, 0))
          .when(mock)
          .invokeWithRetry(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Predicate.class));
    })) {
      textJoinerAI = new TextJoinerAI("en", "A story about a cat.");
    }

    int choice = textJoinerAI.chooseText(new String[]{"Cat jumped and played.", "A cat jumped and played."}).content();

    Assertions.assertEquals(1, choice);
  }

  @Test
  public void testFix() {
    TextJoinerAI textJoinerAI;

    try (MockedConstruction<AgentBase> mocked = Mockito.mockConstruction(AgentBase.class, (mock, context) -> {
      Mockito.doReturn(new ProcessingResult<>("A cat jumped and played.", 0, 0, 0))
          .when(mock)
          .invokeWithRetry(Mockito.anyString(), Mockito.anyInt());
    })) {
      textJoinerAI = new TextJoinerAI("en", "A story about a cat.");
    }

    String answer = textJoinerAI.fixText("a C1at jumped a1nd pla.yed.").content();

    Assertions.assertEquals("A cat jumped and played.", answer);
  }
}
