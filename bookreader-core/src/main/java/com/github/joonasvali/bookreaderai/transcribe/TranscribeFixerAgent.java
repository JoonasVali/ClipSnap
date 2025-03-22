package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.Constants;
import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import com.openai.models.CompletionUsage;

public class TranscribeFixerAgent {
  private static final String SYSTEM_PROMPT = """
        You are a professional Transcriber. ${LANGUAGE}Make your best judgement to detect whether the
        transcribed words are damaged or mistyped. You are smart and can detect missing pieces from context as well.
        Do not change style. You need to keep the historical context of the story intact.
        ${STORY} Based on context, can you find ALL the words that would need fixing? be thorough with your
        search, but precise. When you change a word, make sure it's syntactically similar, otherwise it probably
        is not the right word. If you are unsure, leave it as is. Try to be short in your descriptions. Good luck!
      """;

  private static final String SYSTEM_PROMPT_FINAL = """
        Only output the fixed content fully, nothing else, no surrounding quotes or explanations.
      """;

  private final String languageDirection;
  private final String story;
  private final CompletionUsage ZERO_USAGE = CompletionUsage.builder().completionTokens(0).totalTokens(0).promptTokens(0).build();

  public TranscribeFixerAgent(String language, String story) {
    if (language != null) {
      this.languageDirection = "The content is in " + language + " mostly.";
    } else {
      this.languageDirection = "";
    }
    this.story = story;
  }

  public ProcessingResult<String> fix(String text) {
    OpenAIClient client = OpenAIOkHttpClient.builder()
        .apiKey(System.getenv(Constants.OPENAI_API_KEY_ENV_VARIABLE))
        .build();

    var builder = ChatCompletionCreateParams.builder()
        .addUserMessage(SYSTEM_PROMPT
            .replace("${LANGUAGE}", languageDirection)
            .replace("${STORY}", story))
        .addUserMessage("\"" + text + "\"")
        .model(ChatModel.O1_PREVIEW_2024_09_12);


    ChatCompletionCreateParams params = builder.build();
    ChatCompletion chatCompletion = client.chat().completions().create(params);
    var firstResult = chatCompletion.choices().getFirst();
    var firstUsage = chatCompletion.usage().orElse(ZERO_USAGE);

    var firstOutput = firstResult.message().content().orElse("-No content-");

    if (firstOutput.equals("-No content-")) {
      throw new RuntimeException("Failed to fix the content.");
    }

    var finalBuilder = ChatCompletionCreateParams.builder()
        .addUserMessage(text)
        .addUserMessage(firstOutput)
        .addUserMessage(SYSTEM_PROMPT_FINAL)
        .model(ChatModel.GPT_4O_MINI);

    ChatCompletionCreateParams finalParams = finalBuilder.build();
    ChatCompletion finalChatCompletion = client.chat().completions().create(finalParams);
    var secondUsage = finalChatCompletion.usage().orElse(ZERO_USAGE);
    var finalResult = finalChatCompletion.choices().getFirst();
    return new ProcessingResult<>(finalResult.message().content().orElse("-No content-"),
        firstUsage.totalTokens() + secondUsage.totalTokens(),
        firstUsage.promptTokens() + secondUsage.promptTokens(),
        firstUsage.completionTokens() + secondUsage.completionTokens()
    );
  }

}
