package com.example.moodlog.domain.analysis.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
public class ClaudeApiService {

    @Value("${claude.api.key}")
    private String apiKey;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();
    }

    public String analyze(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", "claude-haiku-4-5-20251001",
                "max_tokens", 1024,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            Map response = webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            List<Map> content = (List<Map>) response.get("content");
            return (String) content.get(0).get("text");

        } catch (WebClientResponseException  e) {
            throw new RuntimeException("AI 분석 요청이 시간을 초과했습니다. 잠시 후 다시 시도해주세요.");
        } catch (Exception e) {
            throw new RuntimeException("AI 서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
