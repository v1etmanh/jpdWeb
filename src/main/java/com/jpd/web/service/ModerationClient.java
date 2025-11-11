package com.jpd.web.service;
import com.jpd.web.dto.ModerationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
@Service
public class ModerationClient {
    private final WebClient webClient;
    public ModerationClient(
            @Value("${moderation.api.url}") String apiUrl,
            @Value("${moderation.connect.timeoutMs:3000}") long connectTimeoutMs,
            @Value("${moderation.read.timeoutMs:10000}") long readTimeoutMs
    ) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(readTimeoutMs));

        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public ModerationResponse check(byte[] imageBytes) {
        String b64 = Base64.getEncoder().encodeToString(imageBytes);
        WebClient.RequestHeadersSpec<?> req = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("image_base64", b64));

        return req.retrieve()
                .toEntity(ModerationResponse.class)
                .map(responseEntity -> {
                    // Status 200 => ALLOWED, 403 => BLOCKED (theo Lambda)
                    ModerationResponse body = responseEntity.getBody();
                    if (body == null) {
                        ModerationResponse fallback = new ModerationResponse();
                        fallback.setDecision("BLOCKED");
                        fallback.setReason("EmptyResponse");
                        return fallback;
                    }
                    return body;
                })
                .onErrorResume(ex -> {
                    // Fail-safe: lỗi mạng cũng coi như BLOCKED
                    ModerationResponse error = new ModerationResponse();
                    error.setDecision("BLOCKED");
                    error.setReason("ModerationError:" + ex.getMessage());
                    return reactor.core.publisher.Mono.just(error);
                })
                .block();
    }
}
