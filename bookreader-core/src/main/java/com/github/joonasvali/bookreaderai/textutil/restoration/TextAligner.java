package com.github.joonasvali.bookreaderai.textutil.restoration;

import com.github.joonasvali.bookreaderai.textutil.SentencePotentialMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The TextAligner class is responsible for aligning multiple text inputs.
 * Given an array of String texts, it computes an aligned (or consensus) text
 * using a word-by-word majority vote approach.
 */
public class TextAligner {

  /**
   * Encapsulates the result of an alignment operation.
   * It contains the aligned text and optional meta information.
   */
  public static class AlignmentResult {
    private final String alignedText;
    private final boolean success;

    public AlignmentResult(String alignedText, boolean success) {
      this.alignedText = alignedText;
      this.success = success;
    }

    public String getAlignedText() {
      return alignedText;
    }

    public boolean isSuccess() {
      return success;
    }
  }


  public AlignmentResult alignTexts(String[] textVersions) {
    if (textVersions == null || textVersions.length == 0) {
      return new AlignmentResult("", false);
    }
    List<AlignState> stateList = new ArrayList<>();
    Arrays.stream(textVersions).forEach(text -> {
      String[] sentences = new TextSentenceSplitter().getSentences(text);

      stateList.add(new AlignState(sentences));
    });

    States states = new States(stateList);
    initialize(states);

    int maxSentenceCount = stateList.stream().mapToInt(state -> state.sentences.length).max().orElse(0);

    StringBuilder finalText = new StringBuilder();
    MajorityVoter majorityVoter = new MajorityVoter();
    for (int i = 0; i < maxSentenceCount; i++) {
      List<String> currentSentences = new ArrayList<>();
      for (AlignState state : stateList) {
        if (state.readingIndex < state.sentences.length) {
          state.result.add(state.getCurrentSentence());
          currentSentences.add(state.getCurrentSentence());
          state.readingIndex++;
        }
      }
      String[] stc = currentSentences.toArray(new String[0]);
      MajorityVoter.VoteResult voteResult = majorityVoter.vote(stc);
      if (voteResult.isSuccess()) {
        finalText.append(voteResult.getResultingText());
        if (!finalText.isEmpty() && finalText.charAt(finalText.length() - 1) != '\n') {
          finalText.append(" ");
        }
      }
    }
    if (!finalText.isEmpty() && finalText.charAt(finalText.length() - 1) == ' ') {
      finalText.replace(finalText.length() - 1, finalText.length(), "");
    }


    return new AlignmentResult(finalText.toString(), true);
  }

  record Record(String text, int index, AlignState alignState) {
  }

  record ComparisonResult(int index1, int index2, AlignState alignState1, AlignState alignState2, float score) {

  }

  public void initialize(States states) {
    SentencePotentialMatcher matcher = new SentencePotentialMatcher();

    List<Record> records = new ArrayList<>();

    states.states.forEach(textVersion -> {
      for(int i = 0; i < textVersion.sentences.length; i++) {
        records.add(new Record(textVersion.sentences[i], i, textVersion));
      }
    });

    List<ComparisonResult> results = new ArrayList<>();
    records.forEach(record1 -> {
      records.forEach(record2 -> {
        // Skip duplicates
        if (Objects.toIdentityString(record1.alignState).hashCode() >= Objects.toIdentityString(record2.alignState).hashCode()) {
          return;
        }
        SentencePotentialMatcher.MatchResult result = matcher.match(record1.text, record2.text);
        if (result.score > 0.3) {
          results.add(new ComparisonResult(record1.index, record2.index, record1.alignState, record2.alignState, result.score));
        }
      });
    });

    int maximumIndex = results.stream().mapToInt(result -> Math.max(result.index1, result.index2)).max().orElse(0);
    Map<AlignState, Integer> stateIndexMap = new HashMap<>();
    for (int i = 0; i < maximumIndex; i++) {
      for (ComparisonResult result : results) {
        if (result.index1 == i || result.index2 == i) {
          Integer previousIndex = stateIndexMap.get(result.alignState1);
          if (previousIndex == null || previousIndex > result.index1) {
            stateIndexMap.put(result.alignState1, result.index1);
          }

          previousIndex = stateIndexMap.get(result.alignState2);
          if (previousIndex == null || previousIndex > result.index2) {
            stateIndexMap.put(result.alignState2, result.index2);
          }
        }
      }
      if (stateIndexMap.size() == states.states.size()) {
        break;
      } else {
        stateIndexMap.clear();
      }
    }

    stateIndexMap.forEach(AlignState::setIndex);
  }

  class States {
    private List<AlignState> states;

    public States(List<AlignState> states) {
      this.states = states;
    }
  }


  static class AlignState {
    private String[] sentences;
    private int readingIndex;
    private int writingIndex;
    private List<String> result;

    public AlignState(String[] sentences) {
      this.sentences = sentences;
      this.readingIndex = 0;
      this.result = new ArrayList<>();
    }

    public String getCurrentSentence() {
      return sentences[readingIndex];
    }

    public void setIndex(Integer i) {
      this.readingIndex = i;
    }
  }
}
