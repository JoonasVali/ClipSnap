package com.github.joonasvali.bookreaderai.textutil;

import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.transcribe.JoinedTranscriber;
import com.github.joonasvali.bookreaderai.transcribe.SimpleTranscriberAgent;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JoinedTranscriberTest {

  /**
   * Invokes the transcribeImages method on JoinedTranscriber.
   * Each created SimpleTranscriberAgent instance will return the corresponding text from the texts array.
   *
   * @param texts variable number of transcription texts to be returned
   * @return the concatenated transcription result
   * @throws IOException if transcribeImages throws one
   */
  public String invokeTest(String... texts) throws IOException {
    // Use an AtomicInteger to count the constructed SimpleTranscriberAgent instances.
    AtomicInteger counter = new AtomicInteger(0);

    try (MockedConstruction<SimpleTranscriberAgent> mocked =
             Mockito.mockConstruction(SimpleTranscriberAgent.class,
                 (mock, context) -> {
                   int count = counter.getAndIncrement();
                   if (count < texts.length) {
                     // Stub the transcribe method with the corresponding text.
                     Mockito.doReturn(CompletableFuture.completedFuture(
                         new ProcessingResult<>(texts[count], 0, 0, 0)
                     )).when(mock).transcribe(Mockito.anyString());
                   } else {
                     // If more instances are created than texts provided, return an empty result.
                     Mockito.doReturn(CompletableFuture.completedFuture(
                         new ProcessingResult<>("", 0, 0, 0)
                     )).when(mock).transcribe(Mockito.anyString());
                   }
                 })) {

      // Create an array of images with a length equal to the number of texts.
      BufferedImage[] images = new BufferedImage[texts.length];
      for (int i = 0; i < texts.length; i++) {
        images[i] = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
      }

      // Instantiate JoinedTranscriber with the created images.
      JoinedTranscriber transcriber = new JoinedTranscriber(images, "english", "story");

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
          The cats are having fun. The dog is having fun too. Listen afafe
          """;

    String text2 = """
          The dog is having fun too. Listen to the birds chirping. This is a beautiful day.
          The sun is shining. The sky is blue. The clouds are white. The birds are singing.
          """;

    String text3 = """
          s.,a. The clouds are white. The birds are singing.
          The kids are playing in the park. The parents are watching them. The kids are having fun.
          """;
    // Expected result is the concatenation of the provided texts.
    String expectedResult = """
        Two cats and a dog are playing in the garden.
        The cats are chasing the dog. The dog is running away from the cats.
        The cats are having fun. The dog is having fun too. Listen to the birds chirping. This is a beautiful day.
        The sun is shining. The sky is blue. The clouds are white. The birds are singing.
        The kids are playing in the park. The parents are watching them. The kids are having fun.
        """;

    String result = invokeTest(text1, text2, text3);
    assertEquals(expectedResult, result);
  }

  @Test
  public void testRepeatingText() throws IOException {
    // Sample text outputs to be used in tests.
    String text1 = """
          Ladybug, ladybug, fly away home.
          The cows are in the meadow. The sheep are in the corn.
          Where is the little
          """;

    String text2 = """
          little logbook? The birds are in the sky. The fish are in the sea.
          The bees are in the hive. The ants are in the ground.
          Yard by yard, life is hard. Inch by inch, life's a cinch.
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
        Where is the little logbook? The birds are in the sky. The fish are in the sea.
        The bees are in the hive. The ants are in the ground.
        Yard by yard, life is hard. Inch by inch, life's a cinch.
        Ladybug, ladybug, fly away home. The cows are in the meadow. The sheep are in the corn.
        Where is the little logbook? The birds are in the sky. The fish are in the sea.
        """;

    String result = invokeTest(text1, text2, text3);
    assertEquals(expectedResult, result);
  }
}
