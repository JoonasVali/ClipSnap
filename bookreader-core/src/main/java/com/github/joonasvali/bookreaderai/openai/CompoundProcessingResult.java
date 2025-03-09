package com.github.joonasvali.bookreaderai.openai;

import java.util.Arrays;

public class CompoundProcessingResult<T> {
  private final T content;

  public CompoundProcessingResult(T content) {
    this.content = content;
  }

  public ProcessingResult<T> calculate(ProcessingResult<T>[] results) {
    return new ProcessingResult<>(content,
        Arrays.stream(results).mapToLong(ProcessingResult::promptTokens).sum(),
        Arrays.stream(results).mapToLong(ProcessingResult::completionTokens).sum(),
        Arrays.stream(results).mapToLong(ProcessingResult::totalTokens).sum()
    );
  }
}
