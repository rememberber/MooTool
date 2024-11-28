package com.luoboduner.moo.tool.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;

public class MicrosoftTranslatorUtil {

    private static final String SUBSCRIPTION_KEY = "YOUR_SUBSCRIPTION_KEY";
    private static final String ENDPOINT = "YOUR_ENDPOINT";
    private static final String LOCATION = "YOUR_RESOURCE_LOCATION";

    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String translate(String text, String from, String to) throws IOException {
        String url = ENDPOINT + "/translate?api-version=3.0&from=" + from + "&to=" + to;

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "[{\"Text\": \"" + text + "\"}]"
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Ocp-Apim-Subscription-Key", SUBSCRIPTION_KEY)
                .addHeader("Ocp-Apim-Subscription-Region", LOCATION)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            JsonNode jsonNode = objectMapper.readTree(response.body().string());
            return jsonNode.get(0).get("translations").get(0).get("text").asText();
        }
    }

    public static void main(String[] args) {
        try {
            String text = "Hello, world!";
            String translatedText = translate(text, "en", "zh-Hans");
            System.out.println("Translated text: " + translatedText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}