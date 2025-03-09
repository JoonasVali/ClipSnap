package com.github.joonasvali.bookreaderai.openai;

public record ProcessingResult<T>(T content, long promptTokens, long completionTokens, long totalTokens) {
}
