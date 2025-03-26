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

import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class AgentBase {
  private Logger logger;

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

  public void setLogger(Logger logger) {
    this.logger = logger;
  }

  public Logger getLogger() {
    return logger;
  }

  public ProcessingResult<String> invokeWithRetry(String text, int maxRetries) {
    return new Retry<ProcessingResult<String>>(maxRetries).runWithRetry(
        () -> invoke(text)
    );
  }

  public ProcessingResult<String> invokeWithRetry(String text, int maxRetries, Predicate<ProcessingResult<String>> predicate) {
    return new Retry<ProcessingResult<String>>(maxRetries).runWithRetry(
        () -> invoke(text), predicate
    );
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

  public class Retry<T> {
    private int maxRetries;

    public Retry(int maxRetries) {
      this.maxRetries = maxRetries;
    }

    public T runWithRetry(Callable<T> runnable) {
      return runWithRetry(runnable, result -> true);
    }

    public T runWithRetry(Callable<T> runnable, Predicate<T> successPredicate) {
      int retries = 0;
      while (retries < maxRetries) {
        if (retries > 0) {
          try {
            Thread.sleep(3000L);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
        try {
          T result = runnable.call();
          if (successPredicate.test(result)) {
            return result;
          }
        } catch (Exception e) {
          logger.warn("Failed to run with retry", e);
        }
        retries++;
      }
      logger.error("Failed to run with retry after {} retries", maxRetries);
      throw new RuntimeException("Unable to complete the operation after " + maxRetries + " retries");
    }
  }
}
