package com.sangchu.embedding.service;

import com.sangchu.embedding.entity.EmbeddingBatchInboundDto;
import com.sangchu.embedding.entity.EmbeddingInboundDto;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import org.springframework.ai.embedding.Embedding;

@Component
public class EmbeddingService {

    @Value("${embed.huggingface.uri}")
    private String uri;

    @Value("${HUGGINGFACE_EMBED_ENDPOINT}")
    private String embedEndpoint;

    @Value("${HUGGINGFACE_BATCH_EMBED_ENDPOINT}")
    private String batchEmbedEndpoint;


    private final RestTemplate restTemplate = new RestTemplate();

    public Embedding getEmbedding(String keyword) {

        Map<String, String> requestBody = Map.of("keyword", keyword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<EmbeddingInboundDto> response = restTemplate.postForEntity(
                uri + embedEndpoint, request, EmbeddingInboundDto.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            float[] vector = response.getBody().getEmbedding();

            return new Embedding(vector, 0);
        } else {
            throw new CustomException(ApiStatus._EMBEDDING_SERVER_ERROR);
        }
    }

    public List<Embedding> getBatchEmbeddings(List<String> keywords) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> requestBody = Map.of("keywords", keywords);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<EmbeddingBatchInboundDto> response = restTemplate.postForEntity(
                uri + batchEmbedEndpoint, request, EmbeddingBatchInboundDto.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            List<float[]> vectors = response.getBody().getEmbeddings();
            return vectors.stream()
                    .map(vec -> new Embedding(vec, 0))
                    .toList();
        } else {
            throw new CustomException(ApiStatus._EMBEDDING_SERVER_ERROR);
        }
    }
}
