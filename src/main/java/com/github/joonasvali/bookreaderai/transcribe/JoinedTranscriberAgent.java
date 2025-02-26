package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.ProgressUpdateUtility;
import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import com.openai.models.CompletionUsage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class JoinedTranscriberAgent {
  private static final String SYSTEM_PROMPT = """
        You will be given a set of texts, which have been transcribed.
        The texts are from the same story. Your task is to combine the texts into one coherent
        text. You can use your best judgement to decide which parts of the texts overlap and
        unify them accordingly. The texts are in correct order, your task is to join them
        and make sure they match, but you should not remove anything meaningful.
      """;

  private final BufferedImage[] images;
  private final String language;
  private final String story;
  private ProgressUpdateUtility progressUpdateUtility;

  public JoinedTranscriberAgent(BufferedImage[] images, String language, String story) {
    this.images = images;
    this.language = language;
    this.story = story;
  }

  public void transcribeImages(Consumer<ProcessingResult<String>> callback) throws IOException {
    SimpleTranscriberAgent[] agents = new SimpleTranscriberAgent[images.length];
    for (int i = 0; i < images.length; i++) {
      agents[i] = new SimpleTranscriberAgent(images[i], language, story);
    }


    List<CompletableFuture<String>> futures = new ArrayList<>();
    for (int i = 0; i < agents.length; i++) {
      int index = i;
      CompletableFuture<String> future = agents[i].transcribe();
      if (progressUpdateUtility != null) {
        future.thenRun(() -> progressUpdateUtility.setTranscribeTaskComplete(index, true));
      }
      futures.add(future);
    }

    CompletableFuture<Void> allDone = CompletableFuture.allOf(
        futures.toArray(new CompletableFuture[0])
    );

    allDone.thenRun(() -> {
      System.out.println("All tasks finished...");
      // All tasks finished. Gather results:
      List<String> results = futures.stream().map(CompletableFuture::join).toList();
      if (progressUpdateUtility != null) {
        progressUpdateUtility.setFinalTaskComplete();
      }
      callback.accept(join(results));
    });

  }

  private ProcessingResult<String> join(List<String> results) {
    System.out.println("Joining results: " + results);
    OpenAIClient client = OpenAIOkHttpClient.fromEnv();

    var builder = ChatCompletionCreateParams.builder()
        .addSystemMessage(SYSTEM_PROMPT)
        .model(ChatModel.GPT_4O_MINI);

    for (String result : results) {
      builder.addUserMessage(result);
    }

    ChatCompletionCreateParams params = builder.build();

    ChatCompletion chatCompletion = client.chat().completions().create(params);

    var firstResult = chatCompletion.choices().getFirst();

    System.out.println("Joined result: " + firstResult.message().content().orElse("-No content-"));
    return new ProcessingResult<>(firstResult.message().content().orElse("-No content-"),
        chatCompletion.usage().map(CompletionUsage::totalTokens).orElse(0L),
        chatCompletion.usage().map(CompletionUsage::promptTokens).orElse(0L),
        chatCompletion.usage().map(CompletionUsage::completionTokens).orElse(0L)
    );
  }

  public void setProgressUpdateUtility(ProgressUpdateUtility progressUpdateUtility) {
    this.progressUpdateUtility = progressUpdateUtility;
  }
}
