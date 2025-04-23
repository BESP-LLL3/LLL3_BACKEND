package com.sangchu.trend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import com.sangchu.global.mapper.ResponseMapper;
import com.sangchu.global.response.BaseResponse;
import com.sangchu.global.util.statuscode.ApiStatus;
import com.sangchu.trend.entity.KeywordSuggestionResponseDto;
import com.sangchu.trend.service.TrendService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(
        origins = "http://localhost:3000",
        allowCredentials = "true"
) // React 포트
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TrendController {
    private final TrendService trendService;

    @Value("${spring.elasticsearch.limit}")
    private int limit;

    @GetMapping("/keyword")
    public ResponseEntity<BaseResponse<KeywordSuggestionResponseDto>> getKeyword(@RequestParam String keyword) {
        KeywordSuggestionResponseDto responseDto = trendService.getKeywordList(keyword, limit);
        return ResponseMapper.successOf(ApiStatus._OK, responseDto, TrendController.class);
    }

    @GetMapping("/trend")
    public ResponseEntity<?> getKeywordTrend(@RequestParam String keyword) {
        // 임시 트렌드 데이터 (값만 배열로 반환)
        List<Double> trendValues = new ArrayList<>();

        // 16개의 랜덤 값 생성 (2020-03부터 2024-06까지 3개월 간격)
        for (int i = 0; i < 16; i++) {
            trendValues.add(Math.random() * 100); // 0-100 사이의 랜덤 값
        }

        Map<String, Object> response = new HashMap<>();
        response.put("values", trendValues);

        return ResponseEntity.ok(response);
    }
}
