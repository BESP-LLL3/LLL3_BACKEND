package com.sangchu.branding.service;


import com.sangchu.branding.entity.BrandNameRequestDto;
import com.sangchu.branding.entity.ChatRequestDto;
import com.sangchu.branding.entity.ChatResponse;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@EnableAsync
public class OpenAiService {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature}")
    private Double temperature;

    @Async
    public CompletableFuture<List<String>> getBrandName(BrandNameRequestDto brandNameRequestDto) {
        String userMessage = buildUserPrompt(brandNameRequestDto);
        String systemMessage = "당신은 창의적인 브랜드 상호명 전문가입니다. " +
                "검색어와 관련된 이름을 추천하되, 반드시 포함할 필요는 없습니다. " +
                "추가 키워드와 트렌드 키워드를 활용하세요. " +
                "자연스럽고 매력적인 한글 상호명을" + brandNameRequestDto.getLimit() + "개 정도 제안해 주세요. " +
                "번호나 설명 없이 상호명만 줄바꿈으로 구분해서 출력해 주세요.";

        // 요청 메시지 작성
        ChatRequestDto request = ChatRequestDto.builder()
                .model(model)
                .messages(List.of(
                        new ChatRequestDto.Message("system", systemMessage),
                        new ChatRequestDto.Message("user", userMessage)
                ))
                .temperature(temperature)
                .build();

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 요청 바디 설정
        HttpEntity<ChatRequestDto> entity = new HttpEntity<>(request, headers);

        // API 호출
        ResponseEntity<ChatResponse> response = restTemplate.exchange(
                baseUrl + "v1/chat/completions",
                HttpMethod.POST,
                entity,
                ChatResponse.class
        );

        // 응답 처리
        if (!response.getBody().getChoices().isEmpty()) {
            String chatResponse = response.getBody().getChoices().getFirst().getMessage().getContent();
            return CompletableFuture.completedFuture(Arrays.asList(chatResponse.split("\n")));
        } else {
            throw new CustomException(ApiStatus._OPENAI_RESPONSE_FAIL);
        }
    }


    public String buildUserPrompt(BrandNameRequestDto brandNameRequestDto) {
        // 유저 메시지
        return "검색어: " + brandNameRequestDto.getSearchWord() + "\n"
                + "트렌드 키워드: " + String.join(", ", brandNameRequestDto.getTrendKeywords()) + "\n"
                + "추가 키워드: " + brandNameRequestDto.getAdditionalKeyword() + "\n"
                + "상호명 추천 개수: " + brandNameRequestDto.getLimit() + "개";
    }
}
