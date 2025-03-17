package com.github.joonasvali.bookreaderai.transcribe;

import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.textutil.StringSelector;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextAligner;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextRestorer;
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

    if (logger.isDebugEnabled()) {
      for (String text : texts) {
        logger.debug("Text: " + text);
      }
    }

    TextRestorer textRestorer = new TextRestorer();
    String restoredText = textRestorer.restoreText(texts);

    return new ProcessingResult<>(restoredText, 0, 0,0);
  }
}
