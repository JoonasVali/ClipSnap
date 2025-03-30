package com.github.joonasvali.bookreaderai;

import com.github.joonasvali.bookreaderai.openai.ImageAnalysis;
import com.github.joonasvali.bookreaderai.openai.ProcessingResult;
import com.github.joonasvali.bookreaderai.transcribe.SimpleTranscriberAgent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleTranscriberAgentTest {

  public String invokeTest(String... texts) {
    // Count the constructed ImageAnalysis instances.
    AtomicInteger counter = new AtomicInteger(0);

    try (MockedConstruction<ImageAnalysis> mocked =
             Mockito.mockConstruction(ImageAnalysis.class,
                 (mock, context) -> {
                   try {
                     Mockito.doReturn(new ProcessingResult<>(texts, 0, 0, 0))
                         .when(mock).process(Mockito.any(BufferedImage.class), Mockito.anyInt());
                   } catch (IOException e) {
                     throw new RuntimeException(e);
                   }
                 })) {

      BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

      SimpleTranscriberAgent transcriber = new SimpleTranscriberAgent(image, "english", "story");

      ProcessingResult<String> result = transcriber.transcribe().get();

      return result.content();
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testWithMinorDifferences() {
    String value = invokeTest("bears, beets, Battlestar Galactica", "bears, beets, Battlestar Galactica.", "bears, bears, Battlestar Galactica");
    Assertions.assertEquals("bears, beets, Battlestar Galactica", value);
  }

  @Test
  public void testWithExtraWords() {
    String value = invokeTest("bears, beets, Battlestar Galactica.", "bears, bears, Battlestar Galactica", "..still. bears, beets, Battle star Galactica");
    Assertions.assertEquals("bears, beets, Battlestar Galactica", value);
  }

  @Test
  public void testMessyText() {
    String value = invokeTest("bears, beets, Five Battlestar Galactica.", "bears, bears, BattlestarGalactica", "..still. bears, four beets, Battle star Galactica");
    Assertions.assertEquals("bears, beets, Five Battlestar Galactica", value);
  }

  @Test
  public void testLongerText() {
    String value = invokeTest(text1, text2, text3);
    Assertions.assertEquals("""
        3. augustil 1941. a.
        
        Läksin tagasi jalaväepolgu staapi. Küsisin sealt ühelt radistilt laetud aku. Kuid ka see oli vist halvasti laetud, või kaua taga juba töötatud, sest side oli katkendlik. Siis läksin õhtul tagasi oma divisoni vaatluspunkti, et tuua sealt laetud akud. Ka võtsin sealt kaasa mitu uut anoodpatareid. Sain siis kahe päeva järel jälle köögist sooja toitu.
        
        Kell 20 algas äge sakslaste pealetung terve rinde ulatuses. Meist lõunapool saavutasid nad edu, lüües augu venelaste rindesse mitme kilomeetri ulatuses. Meie vaatluspunktidel tuli koos jalaväe staapidega taanduda. Siis tuli terve öö liikuda mööda metsi ja põldu raadiojaam seljas. Alles hommikul sain kokku meie pataljoni sideauto- ja meeskonnaga, kes võttis sidet üles.
        """, value);
  }

  String text1 = """
    3. augustil 1941. a.
    Läksin tagasi jalaväepolgu staapi. Küsisin sealt ühelt radistilt laetud aku. Kuid ka see oli vist halvasti laetud, või kaua taga juba töötatud, sest side oli katkendlik. Siis läksin õhtul tagasi oma divisoni vaatluspunkti, et tuua sealt laetud akud. Ka võtsin sealt kaasa mitu uut anoodpatareid. Sain siis kahe päeva järel jälle köögist sooja toitu.
    
    Kell 20 algas äge sakslaste pealetung terve rinde ulatuses. Meist lõunapool saavutasid nad edu, lüües augu venelaste rindesse mitme kilomeetri ulatuses. Meie vaatluspunktidel tuli koos jalaväe staapidega taanduda. Siis tuli terve öö liikuda mööda metsi ja põldu raadiojaam seljas. Alles hommikul sain kokku meie pataljoni sideauto- ja meeskonnaga, kes võttis sidet üles.
    """;

  String text2 = """
    randomtext.
    3. augustil 1941. a.
    
    Läksin tagasi jalaväepolgu staapi. Küsisin sealt ühelt radistilt laetud aku. Kuid ka see oli vist halvasti laetud, või kaua taga juba töötatud, sest side oli katkendlik. Siis läksin õhtul tagasi oma diviisjoni vaatluspunkti, et tuua sealt laetud akud. Ka võtsin sealt kaasa mitu uut anoodpatareid. Sain siis kahe päeva järel jälle köögist sooja toitu.
    
    Kell 20 algas äge sakslaste pealetung terve rinde ulatuses. Meist lõunapool saavutasid nad edu, lüües augu venelaste rindesse mitme kilomeetri ulatuses. Meie vaatluspunktil tuli koos jalaväe staapidega taanduda. Siis tuli terve öö liikuda mööda metsi ja põldu raadiojaam seljas. Alles hommikul sain kokku meie pataljoni sideauto- ja meeskonnaga, kes võttis sidet üles.
    """;

  String text3 = """
    3. augustil 1941. a.
    
    Läksin tagasi jalaväepolgu staapi. Küsisin sealt ühelt radistilt laetud aku. Kuid ka see oli vist halvasti laetud, või kaua taga juba töötatud, sest side oli katkendlik. Siis läksin õhtul tagasi oma divisoni vaatluspunkti, et tuua sealt laetud akku. Ka võtsin sealt kaasa mitu uut anoodpatareid. Sain siis kahe päeva järel jälle köögist sooja toitu.
    
    Kell 20 algas äge sakslaste pealetung terve rinde ulatuses. Meist lõunapool saavutasid nad edu, lüües augu venelaste rindesse mitme kilomeetri ulatuses. Meie vaatluspunktidel tuli koos jalaväe staapidega taanduda. Siis tuli terve öö liikuda mööda metsi ja põldu raadiojaam seljas. Alles hommikul sain kokku meie pataljoni sideauto- ja meeskonnaga, kes võttis sidet üles.
    """;
}
