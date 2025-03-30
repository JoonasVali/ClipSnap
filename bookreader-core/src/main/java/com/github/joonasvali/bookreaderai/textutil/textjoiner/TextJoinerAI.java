package com.github.joonasvali.bookreaderai.textutil.textjoiner;

import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.transcribe.AgentBase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextJoinerAI implements TextJoinerAIExtension {
  private static final Logger logger = LoggerFactory.getLogger(TextJoinerAI.class);
  private static final String FIX_SYSTEM_PROMPT = "This text was joined from multiple parts using algorithm. " +
      "Please make sure there are no merge artifacts. Only output the correct text without explanations or quotes. The text follows:";
  private static final String CHOOSE_SYSTEM_PROMPT = "Please choose the most correct variant of the text. The text follows as a JSON object. " +
      "Only output the numerical index of the correct variant and nothing else, without explanations or quotes. If you are unsure, choose 0.";
  private AgentBase fixAgent;
  private AgentBase chooseTextAgent;

  public TextJoinerAI(String language, String story) {
    this.fixAgent = new AgentBase(FIX_SYSTEM_PROMPT, language, story);
    this.chooseTextAgent = new AgentBase(CHOOSE_SYSTEM_PROMPT, language, story);
    this.fixAgent.setLogger(logger);
    this.chooseTextAgent.setLogger(logger);
  }

  @Override
  public ProcessingResult<String> fixText(String text) {
    return fixAgent.invokeWithRetry(text, 10);
  }

  @Override
  public ProcessingResult<Integer> chooseText(String[] texts) {
    JSONArray array = new JSONArray();
    for (int i = 0; i < texts.length; i++) {
      JSONObject object = new JSONObject();
      object.put("index", i);
      object.put("text", texts[i]);
      array.put(object);
    }
    ProcessingResult<String> result = chooseTextAgent.invokeWithRetry(array.toString(), 5, (content) -> {
      try {
        int choice = Integer.parseInt(content.content());
        return choice >= 0 && choice < texts.length;
      } catch (NumberFormatException e) {
        return false;
      }
    });
    return new ProcessingResult<>(Integer.parseInt(result.content()), result.promptTokens(), result.completionTokens(), result.totalTokens());
  }
}
