package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.openai.models.ChatModel;

public class TranscribeFixerAgent extends AgentBase {
  private static final String SYSTEM_PROMPT = """
        Repair the provided sentence(s). ${LANGUAGE}Make your best judgement to detect whether the
        words are damaged or mistyped.
        
        The following sentence(s) are composed of algorithmically merged text. Make sure it's correct without
        mistakes. Be extremely careful with all sorts of dates and numerical value formats, do not touch them unless
        you know ABSOLUTELY CERTAIN it's a mistake. You can derive correctness of dates from the context as well. 
        If you are unsure, then do not make changes. Only output the fixed content fully, nothing else. Do not output 
        any surrounding quotes or explanations. Additional remarks: ${STORY}
      """;

  public TranscribeFixerAgent(String language, String story) {
    super(SYSTEM_PROMPT, ChatModel.O3_MINI, language, story);
  }

  public ProcessingResult<String> fix(String text) {
    return invoke(text);
  }
}
