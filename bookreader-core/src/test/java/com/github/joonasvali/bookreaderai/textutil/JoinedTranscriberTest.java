package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.agents.ContentJoiner;
import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.transcribe.JoinedTranscriber;
import com.github.joonasvali.bookreaderai.transcribe.SimpleTranscriberAgent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JoinedTranscriberTest {

  /**
   * Invokes the transcribeImages method on JoinedTranscriber.
   * Each created SimpleTranscriberAgent instance will return the corresponding text from the texts array.
   *
   * @param approx the approximated content to be used in the test
   * @param texts variable number of transcription texts to be returned
   * @return the concatenated transcription result
   * @throws IOException if transcribeImages throws one
   */
  public String invokeTest(String approx, String... texts) throws IOException {
    // Use an AtomicInteger to count the constructed SimpleTranscriberAgent instances.
    AtomicInteger counter = new AtomicInteger(0);

    try (MockedConstruction<SimpleTranscriberAgent> mockedSimple =
             Mockito.mockConstruction(SimpleTranscriberAgent.class,
                 (mock, context) -> {
                   int count = counter.getAndIncrement();
                   if (count < texts.length) {
                     // Stub the transcribe method with the corresponding text.
                     Mockito.doReturn(
                         new ProcessingResult<>(texts[count], 0, 0, 0)
                     ).when(mock).transcribe(Mockito.any());
                   } else {
                     // If more instances are created than texts provided, return an empty result.
                     Mockito.doReturn(
                         new ProcessingResult<>("", 0, 0, 0)
                     ).when(mock).transcribe(Mockito.any());
                   }
                 });
         MockedConstruction<ContentJoiner> mockedContentJoiner =
             Mockito.mockConstruction(ContentJoiner.class,
                 (mock, context) -> {
                   Mockito.doAnswer(invocation -> {
                     Object firstArg = invocation.getArgument(0);
                     Object secondArg = invocation.getArgument(1);
                     Assertions.assertEquals(Arrays.toString(texts), Arrays.toString((String[])secondArg));
                     return new ProcessingResult<>(firstArg, 0, 0, 0);
                   }).when(mock).process(Mockito.any(), Mockito.any());
                 });

    ) {

      // Create an array of images with a length equal to the number of texts.
      BufferedImage[] images = new BufferedImage[texts.length];
      for (int i = 0; i < texts.length; i++) {
        images[i] = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
      }

      // Instantiate JoinedTranscriber with the created images.
      JoinedTranscriber transcriber = new JoinedTranscriber(images, "english", "story" , approx, "GPT-4o");

      final StringBuilder result = new StringBuilder();
      // Assume transcribeImages collects the output via the provided Consumer.
      transcriber.transcribeImages(processingResult -> result.append(processingResult.content()));
      return result.toString();
    }
  }

  @Test
  public void testTranscribeImagesReturnsConcatenatedTexts() throws IOException {
    // Sample text outputs to be used in tests.
    String text1 = """
          Two cats and a dog are playing in the garden.
          The cats are chasing the dog. The dog is running away from the cats.
          """;

    String text2 = """
          The dog is having fun too. Listen to the birds chirping. This is a beautiful day.
          The sun is shining. The sky is blue.
          """;

    String text3 = """
          The clouds are white. The birds are singing.
          The kids are playing in the park. The parents are watching them. The kids are having fun.
          """;
    // Expected result is the concatenation of the provided texts.
    String expectedResult = """
        Two cats and a dog are playing in the garden.
        The cats are chasing the dog. The dog is running away from the cats.
        The dog is having fun too. Listen to the birds chirping. This is a beautiful day.
        The sun is shining. The sky is blue.
        The clouds are white. The birds are singing.
        The kids are playing in the park. The parents are watching them. The kids are having fun.
        """;

    String result = invokeTest(expectedResult, text1, text2, text3);
    assertEquals(expectedResult, result);
  }

  @Test
  public void testRepeatingText() throws IOException {
    // Sample text outputs to be used in tests.
    String text1 = """
          Ladybug, ladybug, fly away home.
          The cows are in the meadow. The sheep are in the corn.
          Where is the
          """;

    String text2 = """
          little logbook? The birds are in the sky. The fish are in the sea.
          The bees are in the hive.
          """;

    String text3 = """
          The ants are in the ground. Yard by yard, life is hard. Inch by inch, life's a cinch.
          Ladybug, ladybug, fly away home. The cows are in the meadow. The sheep are in the corn.
          Where is the little logbook? The birds are in the sky. The fish are in the sea.
          """;
    // Expected result is the concatenation of the provided texts.
    String expectedResult = """
        Ladybug, ladybug, fly away home.
        The cows are in the meadow. The sheep are in the corn.
        Where is the
        little logbook? The birds are in the sky. The fish are in the sea.
        The bees are in the hive.
        The ants are in the ground. Yard by yard, life is hard. Inch by inch, life's a cinch.
        Ladybug, ladybug, fly away home. The cows are in the meadow. The sheep are in the corn.
        Where is the little logbook? The birds are in the sky. The fish are in the sea.
        """;

    String result = invokeTest(expectedResult, text1, text2, text3);
    assertEquals(expectedResult, result);
  }
}
