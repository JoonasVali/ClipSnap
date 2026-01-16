package com.github.joonasvali.bookreaderai.agents;

import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextSentenceSplitter;
import com.github.joonasvali.bookreaderai.transcribe.AgentBase;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CorrectorAgent extends AgentBase {
  private static final String systemPrompt = """
      Background: ${STORY} ${LANGUAGE}

      Please review the following transcribed text for any obvious errors, such as misspellings, incorrect punctuation,
      or grammar issues. Do not alter the wording, phrasing, or content of the text; focus solely on ensuring the text 
      is clear, accurate, and free of basic errors. If any mistakes are identified, please provide the corrected version 
      without changing the intended meaning of the text. You output only the corrected text, nothing else - No surrounding
      quotes, no explanations, no meta comments.
      """;
  private static final int MAX_CHARS_PER_BATCH = 1000;
  private static final int WORDS_PER_BATCH = 40;

  public CorrectorAgent(String language, String story) {
    super(systemPrompt, language, story);
  }

  public ProcessingResult<String> correct(String text) {
    TextSentenceSplitter splitter = new TextSentenceSplitter();
    String[] sentences = splitter.getSentences(text);

    StringBuilder correctedText = new StringBuilder();

    long promptTokens = 0;
    long completionTokens = 0;
    long totalTokens = 0;

    StringBuilder batchBuilder = new StringBuilder();
    int wordCount = 0;

    for (int i = 0; i < sentences.length; i++) {
      String sentence = sentences[i];
      if (sentence.isEmpty()) {
        continue; // Skip empty sentences
      }

      boolean endsWithLineBreak = sentence.endsWith("\n") || sentence.endsWith("\r");

      // Break larger sentences into <= MAX_CHARS_PER_BATCH chunks
      List<String> chunks = chunkSentence(sentence, MAX_CHARS_PER_BATCH);

      for (int j = 0; j < chunks.size(); j++) {
        String chunk = chunks.get(j);

        // If adding this chunk would exceed MAX_CHARS_PER_BATCH, flush first.
        if (batchBuilder.length() > 0
            && batchBuilder.length() + 1 + chunk.length() > MAX_CHARS_PER_BATCH) {
          flushBatchIntoCorrectedText(batchBuilder, correctedText, endsWithLineBreak && j == chunks.size() - 1, i < sentences.length - 1);
          wordCount = 0;
        }

        // Add the chunk to the batch builder.
        if (batchBuilder.length() > 0) {
          batchBuilder.append(" ");
        }
        batchBuilder.append(chunk);
        wordCount += chunk.split("\\s+").length;

        // Determine if we should flush:
        // Flush if we've reached the word count threshold (unless more chunks remain for the current sentence)
        // or if it’s the very last chunk of the entire text.
        boolean isLastSentenceInArray = (i == sentences.length - 1 && j == chunks.size() - 1);
        if ((wordCount >= WORDS_PER_BATCH && !isLastSentenceInArray) || isLastSentenceInArray) {
          flushBatchIntoCorrectedText(batchBuilder, correctedText, endsWithLineBreak && j == chunks.size() - 1, i < sentences.length - 1);
          wordCount = 0;
        }
      }
    }

    return new ProcessingResult<>(correctedText.toString(), promptTokens, completionTokens, totalTokens);
  }

  /**
   * Splits a sentence into chunks of maximum size maxLen.
   * If a sentence is smaller than maxLen, you'll get a single-element list.
   * If it's bigger, you'll get a list with multiple chunks (each up to maxLen characters).
   */
  private List<String> chunkSentence(String sentence, int maxLen) {
    List<String> chunks = new ArrayList<>();
    int start = 0;
    while (start < sentence.length()) {
      int end = Math.min(start + maxLen, sentence.length());
      chunks.add(sentence.substring(start, end));
      start = end;
    }
    return chunks;
  }

  /**
   * Flushes the batchBuilder content through processBatch and appends
   * the processed result to correctedText. It restores the original
   * batch's leading and trailing line breaks.
   */
  private void flushBatchIntoCorrectedText(StringBuilder batchBuilder, StringBuilder correctedText, boolean endsWithLineBreak, boolean hasMoreSentences) {
    if (batchBuilder.isEmpty()) {
      return; // Nothing to flush
    }

    String donorText = batchBuilder.toString();
    ProcessingResult<String> result = processBatch(donorText);
    String processedContent = result.content();

    // Restore leading and trailing line breaks from the donor text.
    String correctedTextWithLineBreaks = copyLineBreaks(donorText, processedContent);

    // Append the processed batch with restored line breaks.
    correctedText.append(correctedTextWithLineBreaks);

    // Append a trailing space if there is more text and the batch doesn't end with a line break.
    if (!endsWithLineBreak && hasMoreSentences) {
      correctedText.append(" ");
    }

    // Clear the batch builder for the next batch.
    batchBuilder.setLength(0);
  }

  /**
   * Copies leading and trailing line breaks from donorText to targetText.
   * @param donorText The original text with intended line breaks.
   * @param targetText The text returned from processing that may have trimmed line breaks.
   * @return The targetText with the donorText's leading and trailing line breaks.
   */
  private static String copyLineBreaks(String donorText, String targetText) {
    // Define regex patterns to capture one or more leading and trailing line breaks.
    Pattern leadingPattern = Pattern.compile("^((?:\\r?\\n)+)");
    Pattern trailingPattern = Pattern.compile("((?:\\r?\\n)+)$");

    Matcher leadingMatcher = leadingPattern.matcher(donorText);
    Matcher trailingMatcher = trailingPattern.matcher(donorText);

    String donorLeading = "";
    String donorTrailing = "";

    // Extract leading line breaks from donorText if available.
    if (leadingMatcher.find()) {
      donorLeading = leadingMatcher.group(1);
    }

    // Extract trailing line breaks from donorText if available.
    if (trailingMatcher.find()) {
      donorTrailing = trailingMatcher.group(1);
    }

    // Remove any existing leading and trailing line breaks from targetText.
    String targetBody = targetText.replaceAll("^(\\r?\\n)+", "")
        .replaceAll("(\\r?\\n)+$", "");

    // Concatenate donor line breaks with the trimmed target text.
    return donorLeading + targetBody + donorTrailing;
  }

  // Helper method that processes one batch using invokeWithRetry.
  private ProcessingResult<String> processBatch(String batch) {
    return invokeWithRetry(
        batch,
        5,
        res -> res.content() != null && !res.content().isEmpty()
    );
  }
}
