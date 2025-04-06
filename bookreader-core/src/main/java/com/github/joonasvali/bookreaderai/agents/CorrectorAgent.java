// Example updated CorrectorAgent with batch size AND 1000-character limit checks

package com.github.joonasvali.bookreaderai.agents;

import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextSentenceSplitter;
import com.github.joonasvali.bookreaderai.transcribe.AgentBase;

import java.util.ArrayList;
import java.util.List;

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
        continue; // Skip empty ones
      }

      boolean endsWithLineBreak = sentence.endsWith("\n") || sentence.endsWith("\r");

      // Break larger sentences into <= 1000-char chunks
      List<String> chunks = chunkSentence(sentence, MAX_CHARS_PER_BATCH);

      for (int j = 0; j < chunks.size(); j++) {
        String chunk = chunks.get(j);

        // If adding this chunk would exceed 1000 chars, flush first.
        // (We add +1 if batchBuilder isn't empty, to account for a space.)
        if (batchBuilder.length() > 0
            && batchBuilder.length() + 1 + chunk.length() > MAX_CHARS_PER_BATCH) {
          flushBatchIntoCorrectedText(batchBuilder, correctedText, endsWithLineBreak && j == chunks.size() - 1, i < sentences.length - 1);
          wordCount = 0;
        }

        // Now actually add the chunk to the batch builder
        if (batchBuilder.length() > 0) {
          batchBuilder.append(" ");
        }
        batchBuilder.append(chunk);
        wordCount += chunk.split("\\s+").length;

        // Flush if we've reached 40 words (except if we still have more chunks to process),
        // or if it’s the last sentence chunk in the array.
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

   Splits a sentence into chunks of maximum size maxLen. If a sentence
   is smaller than maxLen, you'll get a single-element list. If it's bigger,
   you'll get a list with multiple chunks (each up to maxLen characters). */
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

   Flushes the batchBuilder content through processBatch and appends
   the processed result to correctedText. Takes into account whether there
   should be a trailing space or not, depending on line breaks or final sentence. */
  private void flushBatchIntoCorrectedText(StringBuilder batchBuilder, StringBuilder correctedText, boolean endsWithLineBreak, boolean hasMoreSentences) {
    if (batchBuilder.isEmpty()) {
      return; // Nothing to flush
    }
    ProcessingResult<String> result = processBatch(batchBuilder.toString());
    correctedText.append(result.content());

    // Only add a trailing space if
    // (a) there's more text/sentences to come,
    // (b) and we do not end with a newline char.
    if (!endsWithLineBreak && hasMoreSentences) {
      correctedText.append(" ");
    }

    // Clear the batch
    batchBuilder.setLength(0);
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


