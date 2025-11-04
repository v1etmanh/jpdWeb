package com.jpd.web.controller.customer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@CrossOrigin(origins = "http://localhost:3000") // Cho phép CORS từ tất cả các nguồn
public class TtsController {

    @GetMapping("/api/tts")
    public ResponseEntity<StreamingResponseBody> textToSpeech(
            @RequestParam String text,
            @RequestParam(defaultValue = "ja-JP") String lang) {

        try { 
            // Mã hóa văn bản cho URL
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            String googleTtsUrl = "https://translate.google.com/translate_tts?ie=UTF-8" +
                    "&q=" + encodedText +
                    "&tl=" + lang +
                    "&client=tw-ob";

            // Tạo kết nối HTTP
            URL url = new URL(googleTtsUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", 
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            // Thiết lập response headers
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
            headers.add(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");

            // Tạo streaming response
            StreamingResponseBody responseBody = outputStream -> {
                try {
                    // Đọc từ kết nối và ghi vào output stream
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = connection.getInputStream().read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    connection.disconnect();
                }
            };

            return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}