package com.problemio.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmsGeminiClientTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    private GmsGeminiClient gmsGeminiClient;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        gmsGeminiClient = new GmsGeminiClient(restTemplateBuilder, objectMapper);
        
        ReflectionTestUtils.setField(gmsGeminiClient, "baseUrl", "https://gms.ssafy.io/gmsapi/");
        ReflectionTestUtils.setField(gmsGeminiClient, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(gmsGeminiClient, "model", "gemini-2.0-flash-exp-image-generation");
    }

    @Test
    @DisplayName("Gemini API 호출 및 이미지 파싱 테스트")
    void generatePngBytes_Success() {
        // Given
        String dummyBase64 = Base64.getEncoder().encodeToString("dummy-image-content".getBytes());
        String mockResponseBody = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "inlineData": {
                          "mimeType": "image/png",
                          "data": "%s"
                        }
                      }
                    ]
                  }
                }
              ]
            }
            """.formatted(dummyBase64);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(mockResponseBody, HttpStatus.OK));

        // When
        byte[] result = gmsGeminiClient.generatePngBytes("Test Title", "Test Description", "Test Style");

        // Then
        assertThat(result).isEqualTo("dummy-image-content".getBytes());
    }
}
