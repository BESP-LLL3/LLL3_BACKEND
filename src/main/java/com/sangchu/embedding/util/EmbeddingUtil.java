package com.sangchu.embedding.util;

import com.sangchu.embedding.entity.EmbeddingResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import org.springframework.ai.embedding.Embedding;

public class EmbeddingUtil {

    @Value("${HUGGINGFACE_URI}")
    private static String huggingfaceUri;

    public static Embedding getEmbedding(String keyword) {
        
        String url = huggingfaceUri;

        RestTemplate restTemplate = new RestTemplate();

        Map<String, String> requestBody = Map.of("keyword", keyword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<EmbeddingResponseDto> response = restTemplate.postForEntity(
                url, request, EmbeddingResponseDto.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            float[] vector = response.getBody().getEmbedding();

            return new Embedding(vector, 0);
        } else {
            // TODO: 임베딩 서버 관련 exception 추가
            throw new RuntimeException("임베딩 서버 호출 실패");
        }
    }

}
