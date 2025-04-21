package com.sangchu.elasticsearch;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

@Component
public class ElasticsearchIndexInitializer {

    @Value("${spring.elasticsearch.uris}")
    private String elasticUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void createIndex() {
        String indexName = "my_nori";
        String url = elasticUrl + "/" + indexName;

        // 1. RestTemplate 커스터마이징
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                // 404는 무시
                // TODO: 무시하는 핸들러 만들기
                if (response.getStatusCode() != HttpStatus.NOT_FOUND) {
                    super.handleError(response);
                }
            }
        });

        boolean indexExists;
        try {
            ResponseEntity<String> existsResponse = restTemplate.exchange(
                url, HttpMethod.HEAD, null, String.class
            );
            indexExists = existsResponse.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound e) {
            indexExists = false;
        }
        if (indexExists) {
            System.out.println("Index already exists: " + indexName);
            return;
        }

        Map<String, Object> settings = Map.of(
            "settings", Map.of(
                "analysis", Map.of(
                    "tokenizer", Map.of(
                        "nori_none", Map.of(
                            "type", "nori_tokenizer",
                            "decompound_mode", "none"
                        ),
                        "nori_discard", Map.of(
                            "type", "nori_tokenizer",
                            "decompound_mode", "discard"
                        ),
                        "nori_mixed", Map.of(
                            "type", "nori_tokenizer",
                            "decompound_mode", "mixed"
                        )
                    )
                )
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(settings, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.PUT, request, String.class
        );

        System.out.println("Create index response: " + response.getStatusCode());
    }

}