package com.github.joonasvali.bookreaderai.openai;

public record ProcessingResult<T>(T text, long promptTokens, long completionTokens, long totalTokens) {
}
