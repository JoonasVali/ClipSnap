package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.Constants;
import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import com.openai.models.CompletionUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentBase {
  private final Logger logger;

  private final CompletionUsage ZERO_USAGE = CompletionUsage.builder().completionTokens(0).totalTokens(0).promptTokens(0).build();
  private final String systemPrompt;
  private final ChatModel chatModel;

  public AgentBase(String systemPrompt, String language, String story) {
    this(systemPrompt, ChatModel.CHATGPT_4O_LATEST, language, story);
  }

  public AgentBase(String systemPrompt, ChatModel chatModel, String language, String story) {
    this.logger = LoggerFactory.getLogger(getClass());
    this.chatModel = chatModel;
    String languageDirection;
    if (language != null) {
      languageDirection = "The content is probably in " + language + ".";
    } else {
      languageDirection = "";
    }
    this.systemPrompt = systemPrompt
        .replace("${LANGUAGE}", languageDirection)
        .replace("${STORY}", story);
  }

  public ProcessingResult<String> invoke(String text) {
    OpenAIClient client = OpenAIOkHttpClient.builder()
        .apiKey(System.getenv(Constants.OPENAI_API_KEY_ENV_VARIABLE))
        .build();

    var builder = ChatCompletionCreateParams.builder()
        .addUserMessage(systemPrompt)
        .addUserMessage(text)
        .model(chatModel);


    ChatCompletionCreateParams params = builder.build();
    ChatCompletion chatCompletion = client.chat().completions().create(params);
    var result = chatCompletion.choices().getFirst();

    var output = result.message().content().orElse("-No content-");

    if (output.equals("-No content-")) {
      logger.warn("Unable to fix the text: " + text);
      return new ProcessingResult<>(text, 0, 0, 0);
    }

    return new ProcessingResult<>(output,
        chatCompletion.usage().orElse(ZERO_USAGE).promptTokens(),
        chatCompletion.usage().orElse(ZERO_USAGE).completionTokens(),
        chatCompletion.usage().orElse(ZERO_USAGE).totalTokens()
    );
  }
}
