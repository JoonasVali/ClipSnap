package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.openai.ImageAnalysis;
import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

public class SimpleTranscriberAgent {
  private static final Logger logger = LoggerFactory.getLogger(SimpleTranscriberAgent.class);

  private static final String SYSTEM_PROMPT = """
        You are a professional Transcriber. Transcribe image and give the text back without explanation.
        ${LANGUAGE}Make your best judgement to detect the words in the picture if they
        are damaged. You are smart and can detect missing pieces from context as well.
        ${STORY}Avoid adding asterisks or format unless they are part of the text, but keep the linebreaks in the text! 
        Only output the transcribed text, nothing else. Remember, do not write explanations or meta comments.
      """;

  private final String languageDirection;
  private final BufferedImage bufferedImage;
  private final String story;
  private final String language;
  private final int samples;
  private final String gptModel;

  public SimpleTranscriberAgent(BufferedImage bufferedImage, String language, String story, int samples, String gptModel) {
    this.bufferedImage = bufferedImage;
    if (language != null) {
      this.languageDirection = "The content is in " + language + " mostly.";
    } else {
      this.languageDirection = "";
    }
    this.story = story + " ";
    this.language = language;
    this.samples = samples;
    this.gptModel = gptModel;
  }

  public SimpleTranscriberAgent(BufferedImage bufferedImage, String language, String story) {
    this(bufferedImage, language, story, 3, "GPT-4o");
  }

  public ProcessingResult<String> transcribe(String previousTranscription) {
    ImageAnalysis imageAnalysis = createImageAnalysis(previousTranscription);
    try {
      ProcessingResult<String[]> results = processImage(imageAnalysis);

      ProcessingResult<String> result;
      if (results.content().length > 1) {
        TranscriptionVerifierAgent verifierAgent = new TranscriptionVerifierAgent(language, story);
        result = verifierAgent.verify(results.content());
      } else {
        result = new ProcessingResult<>(results.content()[0], 0, 0, 0);
      }

      return new ProcessingResult<>(result.content(),
          result.promptTokens() + results.promptTokens(),
          result.completionTokens() + results.completionTokens(),
          result.totalTokens() + results.totalTokens()
      );
    } catch (Exception e) {
      logger.error("Unable to complete transcription", e);
      throw new RuntimeException(e);
    }
  }


  private ImageAnalysis createImageAnalysis(String previousTranscription) {
    String prompt = SYSTEM_PROMPT
        .replace("${LANGUAGE}", languageDirection)
        .replace("${STORY}", story) + "\n" + createPromptFromPreviousTranscription(previousTranscription);

    return new ImageAnalysis(prompt, gptModel);
  }

  private String createPromptFromPreviousTranscription(String previousTranscription) {
    if (previousTranscription == null) {
      return "";
    }
    return "In this case you are continuing with a next slice of ongoing transcription. " +
        "Introduce a line break to the beginning if needed. Avoid transcribing text that's already transcribed and separated by red line on top of the image. " +
        "Previous transcription ended with: ..." + getLastSentenceOrMaxOfNWords(previousTranscription);
  }

  private String getLastSentenceOrMaxOfNWords(String text) {
    final int MAX_WORDS = 6;

    if (text == null || text.isEmpty()) {
      return "";
    }

    // Split the text into sentences. This regex splits after ., !, or ? followed by one or more spaces.
    String[] sentences = text.split("(?<=[.!?])\\s+");

    // Use the last sentence if there are sentences; if not, use the original text.
    String candidate = sentences.length > 0 ? sentences[sentences.length - 1] : text;
    candidate = candidate.trim();

    // Split the candidate into words
    String[] words = candidate.split("\\s+");

    // If the candidate sentence has more words than MAX_WORDS, return only the last MAX_WORDS words.
    if (words.length > MAX_WORDS) {
      int start = words.length - MAX_WORDS;
      String[] lastWords = Arrays.copyOfRange(words, start, words.length);
      return String.join(" ", lastWords);
    } else {
      return candidate;
    }
  }

  private ProcessingResult<String[]> processImage(ImageAnalysis imageAnalysis) throws IOException {
    return imageAnalysis.process(bufferedImage, samples);
  }
}
