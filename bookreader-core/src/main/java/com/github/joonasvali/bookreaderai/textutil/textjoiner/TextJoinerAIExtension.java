package com.github.joonasvali.bookreaderai.textutil.textjoiner;

import com.github.joonasvali.bookreaderai.openai.ProcessingResult;

public interface TextJoinerAIExtension {
  ProcessingResult<String> fixText(String text);
  ProcessingResult<Integer> chooseText(String[] texts);
}
