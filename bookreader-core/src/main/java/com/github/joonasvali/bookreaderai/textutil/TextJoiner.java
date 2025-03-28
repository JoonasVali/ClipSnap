package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.textutil.restoration.TextSentenceSplitter;
import com.github.joonasvali.bookreaderai.textutil.textjoiner.PotentialResult;
import com.github.joonasvali.bookreaderai.textutil.textjoiner.TextJoinerAIExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextJoiner {
  private final SentencePotentialMatcher fuzzyMatcher = new SentencePotentialMatcher();
  private final TextJoinerAIExtension extension;

  public TextJoiner(TextJoinerAIExtension extension) {
    this.extension = extension;
  }

  public TextJoiner() {
    this.extension = null;
  }

  public String join(String text1, String text2) {

    String[] sentences1 = new TextSentenceSplitter().getSentences(text1);
    String[] sentences2 = new TextSentenceSplitter().getSentences(text2);

    PotentialResult[] potentialResults = join(
        sentences1,
        sentences2
    );

    if (extension == null) {
      String[] result = potentialResults != null ? potentialResults[0].getSentences() : null;

      if (result == null) {
        result = new String[] { text1, text2 };
      }

      return sentencesToString(result);
    }

    if (potentialResults == null) {
      ProcessingResult<String> result = extension.fixText(sentencesToString(new String[] { text1, text2 }));
      // TODO: tokens ?
      return result.content();
    }

    if (potentialResults.length > 1) {
      String[] texts = new String[potentialResults.length];
      for (int i = 0; i < potentialResults.length; i++) {
        texts[i] = sentencesToString(takeAdjacentSentences(potentialResults[i], potentialResults[i].getCommonSentenceIndex(), true));
      }
      ProcessingResult<Integer> result = extension.chooseText(texts);
      // TODO: tokens ?
      int choice = result.content();
      return sentencesToString(potentialResults[choice].getSentences());
    } else {
      return sentencesToString(potentialResults[0].getSentences());
    }
  }

  private String[] takeAdjacentSentences(PotentialResult potentialResult, int commonSentenceIndex, boolean cut) {
    List<String> result = new ArrayList<>(3);
    // Loop from one sentence before to one sentence after the common sentence.
    for (int i = commonSentenceIndex - 1; i <= commonSentenceIndex + 1; i++) {
      if (i >= 0 && i < potentialResult.getSentences().length) {
        if (cut && i != commonSentenceIndex) {
          result.add(cut(potentialResult.getSentence(i), i == commonSentenceIndex - 1));
        } else {
          result.add(potentialResult.getSentence(i));
        }
      }
    }
    return result.toArray(new String[0]);
  }

  private String cut(String input, boolean cutBefore) {
    int maxWords = 8;
    String[] words = input.split("\\s+");
    if (words.length <= maxWords) {
      return input;
    }
    if (cutBefore) {
      return "..." + String.join(" ", Arrays.copyOfRange(words, words.length - maxWords, words.length));
    } else {
      return String.join(" ", Arrays.copyOfRange(words, 0, maxWords)) + "...";
    }
  }

  private String sentencesToString(String[] sentences) {
    return Arrays.stream(sentences)
        .reduce((s1, s2) -> {
          if (!s1.endsWith("\n") && !s2.startsWith("\n")) {
            return s1 + " " + s2;
          }
          return s1 + s2;
        })
        .orElse("");
  }

  private PotentialResult[] join(String[] sentences1, String[] sentences2) {
    List<PotentialResult> potentialResultList = new ArrayList<>();
    for (int firstSentenceIndex = sentences1.length - 1; firstSentenceIndex >= 0; firstSentenceIndex--) {
      for (int secondSentenceIndex = 0; secondSentenceIndex < sentences2.length; secondSentenceIndex++) {
        String firstSentence = sentences1[firstSentenceIndex];
        String secondSentence = sentences2[secondSentenceIndex];
        // This works in case there's just one sentence in each text.
        SentencePotentialMatcher.MatchResult result = fuzzyMatcher.match(firstSentence, secondSentence);

        int firstOffset = sentences1.length - 1 - firstSentenceIndex;
        int secondOffset = secondSentenceIndex;

        if (firstSentence.startsWith(result.prefix) && secondSentence.endsWith(result.suffix) && (result.score > 0.2 || (firstOffset == 0 && secondOffset == 0)) ) {
          String newSentence = result.prefix + result.commonPart + result.suffix;
          Set<String> candidateCommonSentence = new HashSet<>();
          candidateCommonSentence.add(newSentence);


          if (removePunctuationAndWhiteSpace(result.prefix).isEmpty() && removePunctuationAndWhiteSpace(result.suffix).isEmpty()) {
            candidateCommonSentence.add(firstSentence);
            candidateCommonSentence.add(secondSentence);
          }

          String[] sacrificedSentencesFromFirst = sliceSentences(sentences1, firstSentenceIndex, true);
          String[] sacrificedSentencedFromSecond = sliceSentences(sentences2, secondSentenceIndex, false);

          for (String sentence : candidateCommonSentence) {
            int discardedWordsFirst = countDiscardedWords(firstSentence, sentence);
            int discardedWordsSecond = countDiscardedWords(secondSentence, sentence);

            String[] candidate = buildSentences(sentences1, sentences2, firstSentenceIndex, secondSentenceIndex, sentence);
            PotentialResult potentialResult = new PotentialResult(
                candidate, sentences1.length, sentences2.length, sentence, result.score, Math.max(discardedWordsFirst, discardedWordsSecond), firstSentence, secondSentence,
                sacrificedSentencesFromFirst, sacrificedSentencedFromSecond, firstSentenceIndex
            );
            potentialResultList.add(potentialResult);
          }
        }
      }
    }

    boolean hasOnlyTouchingSentenceWithNoMatch =
        potentialResultList.size() == 1 &&
        // 50%+ of words are discarded as a result of the join
        potentialResultList.getFirst().getDiscardedWords() >= (WordCount.countWords(potentialResultList.getFirst().getCommonSentence()) + potentialResultList.getFirst().getDiscardedWords()) * 0.5f &&
        potentialResultList.getFirst().getFirstTextSentenceOffset() == 0 &&
        potentialResultList.getFirst().getSecondTextSentenceOffset() == 0 &&
        potentialResultList.getFirst().getCalculatedScore() < 0.5f;

    if (potentialResultList.isEmpty() || hasOnlyTouchingSentenceWithNoMatch) {
      return null;
    }

    double maxScore = potentialResultList
        .stream()
        .mapToDouble(PotentialResult::getCalculatedScore).max().orElse(0);

    if (maxScore < 0.11) {
      // With so low score we have no confidence in the result. Let's just append.
      return null;
    }

    // Get the ones with max score:
    PotentialResult[] maxScored = potentialResultList
        .stream()
        .filter(r -> r.getCalculatedScore() >= maxScore - 0.00001)
        .toArray(PotentialResult[]::new);

    return maxScored;
  }

  public String[] sliceSentences(String[] sentences, int indexExcluding, boolean forward) {
    if (forward) {
      return Arrays.copyOfRange(sentences, indexExcluding + 1, sentences.length);
    } else {
      return Arrays.copyOfRange(sentences, 0, indexExcluding);
    }
  }


  private int countDiscardedWords(String sentence, String match) {
    if (sentence == null || match == null) {
      return 0;
    }
    // Remove punctuation and convert to lowercase for both the sentence and the match string.
    String cleanedSentence = sentence.replaceAll("[\\p{Punct}]", "").toLowerCase();
    String cleanedMatch = match.replaceAll("[\\p{Punct}]", "").toLowerCase();

    // Split the cleaned sentence by whitespace.
    String[] words = cleanedSentence.split("\\s+");
    String[] matchingWords = cleanedMatch.split("\\s+");

    return Math.max(0, words.length - matchingWords.length);
  }


  private String removePunctuationAndWhiteSpace(String prefix) {
    return prefix.replaceAll("[^a-zA-Z0-9]", "");
  }

  private String[] buildSentences(String[] sentences1, String[] sentences2, int firstSentenceIndex, int secondSentenceIndex, String commonSentence) {
    String[] returnedSentences = new String[firstSentenceIndex + sentences2.length - secondSentenceIndex];


    for (int i = 0; i < firstSentenceIndex; i++) {
      returnedSentences[i] = sentences1[i];
    }

    returnedSentences[firstSentenceIndex] = commonSentence;

    for (int i = 0; i < sentences2.length - (secondSentenceIndex + 1); i++) {
      returnedSentences[firstSentenceIndex + 1 + i] = sentences2[secondSentenceIndex + i + 1];
    }

    return returnedSentences;
  }
}


