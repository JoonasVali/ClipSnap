package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.textutil.StringSelector;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextAligner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TranscriptionVerifierAgent {
  private Logger logger = LoggerFactory.getLogger(TranscriptionVerifierAgent.class);

  private final String language;
  private final String story;


  public TranscriptionVerifierAgent(String language, String story) {
    this.language = language;
    this.story = story;
  }


  public ProcessingResult<String> verify(String[] texts) {
    logger.debug("TranscriptionVerifierAgent: verify " + texts.length + " texts");
    StringSelector stringSelector = new StringSelector();

    if (logger.isDebugEnabled()) {
      for (String text : texts) {
        logger.debug("Text: " + text);
      }
    }

    String bestMatch = stringSelector.getMostSimilar(texts);

    logger.debug("Best match: " + bestMatch);

    if (bestMatch == null) {
      TextAligner textAligner = new TextAligner();
      var output = textAligner.alignTexts(texts);
      if (!output.isSuccess()) {
        return new ProcessingResult<>(texts[0], 0, 0, 0);
      } else {
        return new ProcessingResult<>(output.getAlignedText(), 0, 0, 0);
      }
    } else {
      return new ProcessingResult<>(bestMatch, 0, 0,0);
    }


  }
}
