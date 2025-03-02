package com.github.joonasvali.bookreaderai.openai;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/*
 * The official OpenAI API client does not allow sending images, so this is a temporary workaround
 * to get image recognition.
 *
 * https://github.com/openai/openai-java/issues/191
 *
 * This class is responsible for sending an image to OpenAI's API and receiving a text response.
 *
 */
public class ImageAnalysis {
  public static final String COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
  public static final String OPENAI_API_KEY = "OPENAI_API_KEY";

  private final String prompt;
  public ImageAnalysis(String prompt) {
    this.prompt = prompt;
  }

  private static String convertBufferedImageToBase64(BufferedImage image, String format) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, format, outputStream);
    byte[] imageBytes = outputStream.toByteArray();
    return Base64.getEncoder().encodeToString(imageBytes);
  }


  public ProcessingResult<String> process(BufferedImage bufferedImage) throws IOException {
    String base64Image = convertBufferedImageToBase64(bufferedImage, "jpg");
    JSONObject jsonBody = createJsonPayload(base64Image, 1);
    String result =  sendRequestToOpenAI(jsonBody);
    JSONObject jsonObject = new JSONObject(result);
    JSONArray choices = jsonObject.getJSONArray("choices");
    JSONObject choice = choices.getJSONObject(0);
    JSONObject message = choice.getJSONObject("message");
    JSONObject usage = jsonObject.getJSONObject("usage");
    int totalTokens = usage.getInt("total_tokens");
    int promptTokens = usage.getInt("prompt_tokens");
    int completionTokens = usage.getInt("completion_tokens");
    return new ProcessingResult<>(message.getString("content"), totalTokens, promptTokens, completionTokens);
  }

  public ProcessingResult<String[]> process(BufferedImage bufferedImage, int answers) throws IOException {
    String base64Image = convertBufferedImageToBase64(bufferedImage, "jpg");
    JSONObject jsonBody = createJsonPayload(base64Image, answers);
    String result =  sendRequestToOpenAI(jsonBody);
    JSONObject jsonObject = new JSONObject(result);
    JSONArray choices = jsonObject.getJSONArray("choices");
    String[] results = new String[answers];
    for (int i = 0; i < answers; i++) {
      JSONObject choice = choices.getJSONObject(i);
      JSONObject message = choice.getJSONObject("message");
      results[i] = message.getString("content");
    }
    JSONObject usage = jsonObject.getJSONObject("usage");
    int totalTokens = usage.getInt("total_tokens");
    int promptTokens = usage.getInt("prompt_tokens");
    int completionTokens = usage.getInt("completion_tokens");
    return new ProcessingResult<>(results, totalTokens, promptTokens, completionTokens);
  }

  public JSONObject createJsonPayload(String base64Image, int n) {
    JSONObject jsonBody = new JSONObject();
    jsonBody.put("model", "chatgpt-4o-latest");
    jsonBody.put("max_tokens", 10000);
    jsonBody.put("n", n);

    JSONArray messages = new JSONArray();

    JSONObject userMessage = new JSONObject();
    userMessage.put("role", "user");

    JSONArray contentArray = new JSONArray();
    contentArray.put(new JSONObject().put("type", "text").put("text", prompt));
    contentArray.put(new JSONObject().put("type", "image_url").put("image_url", new JSONObject().put("url", "data:image/jpeg;base64," + base64Image)));

    userMessage.put("content", contentArray);
    messages.put(userMessage);

    jsonBody.put("messages", messages);
    return jsonBody;
  }

  public static String sendRequestToOpenAI(JSONObject jsonBody) throws IOException {
    OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build();

    RequestBody requestBody = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));

    Request request = new Request.Builder()
        .url(COMPLETIONS_URL)
        .header("Authorization", "Bearer " + System.getenv(OPENAI_API_KEY))
        .header("Content-Type", "application/json")
        .post(requestBody)
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful() && response.body() != null) {
        return response.body().string();
      } else {
        return "Error: " + response.code() + " - " + response.message();
      }
    }
  }
}
