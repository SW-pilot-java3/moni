package com.moni.api.domain.report.client;

import com.moni.api.domain.report.exception.ReportErrorCode;
import com.moni.api.global.error.exception.CustomException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 이상탐지·일별 리포트 공용 Claude API 클라이언트.
 * output_config.format(json_schema)으로 구조화 JSON 응답을 강제해 지정한 타입으로 파싱한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeReportClient {

    private final RestClient claudeRestClient;
    private final ObjectMapper objectMapper;

    public <T> T generate(String model, int maxTokens, String systemPrompt, String userPrompt,
                           Map<String, Object> jsonSchema, Class<T> responseType) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt)),
                "output_config", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "schema", jsonSchema
                        )
                )
        );

        JsonNode response;
        try {
            response = claudeRestClient.post()
                    .uri("/v1/messages")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.error("Claude API 호출 실패 - model={}", model, e);
            throw new CustomException(ReportErrorCode.CLAUDE_API_CALL_FAILED);
        }

        String text = extractText(response);

        try {
            return objectMapper.readValue(text, responseType);
        } catch (JacksonException e) {
            log.error("Claude 응답 JSON 파싱 실패 - model={}, text={}", model, text, e);
            throw new CustomException(ReportErrorCode.CLAUDE_RESPONSE_PARSE_FAILED);
        }
    }

    private String extractText(JsonNode response) {
        if (response == null) {
            throw new CustomException(ReportErrorCode.CLAUDE_API_CALL_FAILED);
        }

        JsonNode content = response.path("content");
        if (!content.isArray() || content.isEmpty()) {
            throw new CustomException(ReportErrorCode.CLAUDE_API_CALL_FAILED);
        }

        return content.get(0).path("text").asString();
    }
}
