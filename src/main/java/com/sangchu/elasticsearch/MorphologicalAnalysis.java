package com.sangchu.elasticsearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class MorphologicalAnalysis {

    private final RestTemplate restTemplate = new RestTemplate();

        @Value("${spring.elasticsearch.uris}")
        private String elasticUrl;

        public List<String> extractNouns(String text) {
            String analyzeUrl = elasticUrl + "/_analyze";

            // 요청 본문
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("tokenizer", "nori_tokenizer");

            // 필요 시 필터 지정 가능 (e.g. nori_part_of_speech)
            requestBody.put("filter", List.of("nori_part_of_speech"));
            requestBody.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                analyzeUrl,
                HttpMethod.POST,
                request,
                Map.class
            );

            List<Map<String, Object>> tokens = (List<Map<String, Object>>) response.getBody().get("tokens");

            // 명사(예: NNG, NNP 등)만 필터링
            Set<String> allowedPOS = Set.of("NNG", "NNP","NP", "NR", "VA", "MM", "MAG", "IC", "XPN", "UNKNOWN");
            return tokens.stream()
                .filter(token -> {
                    String pos = (String) token.get("type");
                    return allowedPOS.contains(pos);
                })
                .map(token -> (String) token.get("token"))
                .collect(Collectors.toList());
        }
}
