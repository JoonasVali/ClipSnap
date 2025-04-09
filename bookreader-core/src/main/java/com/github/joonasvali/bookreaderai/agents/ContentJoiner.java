package com.github.joonasvali.bookreaderai.agents;

import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.transcribe.AgentBase;
import com.openai.models.ChatModel;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ContentJoiner extends AgentBase {
  private final static Logger logger = LoggerFactory.getLogger(ContentJoiner.class);
  private final static String prompt =  "You are a content joiner. Your task is to combine the following content into a single coherent text. " +
      "${LANGUAGE} ${STORY}. " +
      "Please ensure that the final text is grammatically correct and flows well. " +
      "You will receive the approximate content (with grammatical mistakes) and a list of closer views. " +
      "You need to follow the exact sentence structure of the approximation, and fix any incoherent words or typos based on the " +
      "closer views. Think of it like this, the approximation is a puzzle, and the closer views are the pieces that will help you complete it. " +
      "Even though the input is in JSON, you need to return the approximation as plain text, without any formatting or special characters. " +
      "Do not return any explanations or comments.";

  public ContentJoiner(String language, String story) {
    super(prompt, ChatModel.O3_MINI_2025_01_31, language, story);
  }

  public ProcessingResult<String> process(String approximateContent, String[] closerViews) throws IOException {
    JSONObject mainJson = new JSONObject();
    mainJson.put("approximation", approximateContent);
    mainJson.put("closerViews", closerViews);
    String jsonString = mainJson.toString();
    logger.debug("ContentJoiner: process " + jsonString);

    return invokeWithRetry(jsonString, 5);
  }
}
