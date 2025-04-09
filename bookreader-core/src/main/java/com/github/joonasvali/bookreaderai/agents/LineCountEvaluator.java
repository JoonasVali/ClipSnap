package com.github.joonasvali.bookreaderai.agents;

import com.github.joonasvali.bookreaderai.openai.ImageAnalysis;
import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class LineCountEvaluator {
  private final static Logger logger = LoggerFactory.getLogger(LineCountEvaluator.class);
  private final static String prompt =  "Count the number of text lines in the image. Only output the number, nothing else.";

  public ProcessingResult<Integer> countTextLines(BufferedImage bufferedImage) throws IOException {
    ImageAnalysis analysis = new ImageAnalysis(prompt);
    ProcessingResult<String[]> texts = analysis.process(bufferedImage, 5);

    // parse every result and get average
    int totalLines = 0;
    int count = 0;
    for (String text : texts.content()) {
      try {
        int lines = Integer.parseInt(text);
        totalLines += lines;
        count++;
      } catch (NumberFormatException e) {
        logger.warn("Failed to parse line count from result: {}", text);
      }
    }

    if (count == 0) {
      return new ProcessingResult<>(null, texts.promptTokens(), texts.completionTokens(), texts.totalTokens());
    }

    int averageLines = totalLines / count;
    logger.debug("Average line count: {}", averageLines);
    return new ProcessingResult<>(averageLines, texts.promptTokens(), texts.completionTokens(), texts.totalTokens());
  }
}
