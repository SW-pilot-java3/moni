package com.moni.api.domain.report.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.moni.api.domain.report.exception.ReportErrorCode;
import com.moni.api.global.error.exception.CustomException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class ClaudeReportClientTest {

    private record TestResult(String severity, String summary) {
    }

    private static final Map<String, Object> TEST_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "severity", Map.of("type", "string"),
                    "summary", Map.of("type", "string")
            ),
            "required", java.util.List.of("severity", "summary")
    );

    @Test
    @DisplayName("output_config.format으로 요청을 보내고 구조화 JSON 응답을 지정한 타입으로 파싱한다")
    void generate_parsesStructuredJsonResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.anthropic.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ClaudeReportClient client = new ClaudeReportClient(builder.build(), new ObjectMapper());

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("claude-haiku-4-5"))
                .andExpect(jsonPath("$.max_tokens").value(200))
                .andExpect(jsonPath("$.output_config.format.type").value("json_schema"))
                .andRespond(withSuccess("""
                        {
                          "id": "msg_test",
                          "type": "message",
                          "role": "assistant",
                          "content": [{"type": "text", "text": "{\\"severity\\":\\"WARNING\\",\\"summary\\":\\"CPU 급등\\"}"}],
                          "model": "claude-haiku-4-5",
                          "stop_reason": "end_turn"
                        }
                        """, MediaType.APPLICATION_JSON));

        TestResult result = client.generate(
                "claude-haiku-4-5", 200, "system prompt", "user prompt", TEST_SCHEMA, TestResult.class);

        assertThat(result.severity()).isEqualTo("WARNING");
        assertThat(result.summary()).isEqualTo("CPU 급등");
        server.verify();
    }

    @Test
    @DisplayName("응답 텍스트가 대상 타입과 맞지 않으면 CLAUDE_RESPONSE_PARSE_FAILED 예외를 던진다")
    void generate_invalidJsonText_throwsParseFailedException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.anthropic.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ClaudeReportClient client = new ClaudeReportClient(builder.build(), new ObjectMapper());

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess("""
                        {"content": [{"type": "text", "text": "이건 JSON이 아님"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() ->
                client.generate("claude-haiku-4-5", 200, "system", "user", TEST_SCHEMA, TestResult.class))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ReportErrorCode.CLAUDE_RESPONSE_PARSE_FAILED));
    }
}
