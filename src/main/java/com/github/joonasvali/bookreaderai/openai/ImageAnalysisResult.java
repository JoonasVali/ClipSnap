package com.github.joonasvali.bookreaderai.openai;

public record ImageAnalysisResult<T>(T text, int promptTokens, int completionTokens, int totalTokens) {
}
