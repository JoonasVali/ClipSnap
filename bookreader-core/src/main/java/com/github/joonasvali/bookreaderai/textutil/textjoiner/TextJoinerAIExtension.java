package com.github.joonasvali.bookreaderai.textutil.textjoiner;

public interface TextJoinerAIExtension {
  String fixText(String text);
  int chooseText(String[] texts);
}
