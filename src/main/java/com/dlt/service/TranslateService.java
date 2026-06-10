package com.dlt.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

/**
 * 翻译服务：调用Google翻译免费API，将文本翻译为目标语言
 */
public class TranslateService {

    private static final String TRANSLATE_URL =
        "https://translate.googleapis.com/translate_a/single";

    private final OkHttpClient httpClient;

    public TranslateService() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    }

    /**
     * 翻译文本
     * @param text      要翻译的文本
     * @param targetLang 目标语言代码（如 zh-CN, en, ja）
     * @return 翻译结果，失败返回null
     */
    public String translate(String text, String targetLang) {
        try {
            String encodedText = URLEncoder.encode(text, "UTF-8");
            String url = TRANSLATE_URL
                + "?client=gtx&dt=t"
                + "&sl=auto"
                + "&tl=" + targetLang
                + "&q=" + encodedText;

            Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("翻译API请求失败: " + response.code());
                    return null;
                }

                String responseBody = response.body().string();
                return parseTranslation(responseBody);
            }
        } catch (Exception e) {
            System.err.println("翻译出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解析Google翻译API返回的JSON响应
     * 响应格式: [[[ "翻译文本","原文",null,null,10 ],... ],null,"en"]
     */
    private String parseTranslation(String jsonResponse) {
        try {
            JsonArray array = JsonParser.parseString(jsonResponse).getAsJsonArray();
            JsonArray sentences = array.get(0).getAsJsonArray();

            StringBuilder result = new StringBuilder();
            for (JsonElement sentence : sentences) {
                JsonArray parts = sentence.getAsJsonArray();
                if (parts.size() > 0 && !parts.get(0).isJsonNull()) {
                    result.append(parts.get(0).getAsString());
                }
            }
            return result.toString();
        } catch (Exception e) {
            System.err.println("解析翻译结果失败: " + e.getMessage());
            return null;
        }
    }
}
