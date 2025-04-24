package com.sangchu.trend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
    public ResponseEntity<BaseResponse<List<Map<String, Object>>>> getKeywordTrend(@RequestParam String keyword) throws IOException {
        return ResponseMapper.successOf(ApiStatus._OK, trendService.getQuarterlyWordFrequencyAcrossIndices(keyword), TrendController.class);
    }
}
