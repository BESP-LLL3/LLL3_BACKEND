package com.sangchu.trend.controller;

import com.sangchu.embedding.service.EmbeddingService;
import com.sangchu.trend.entity.KeywordInfo;
import com.sangchu.trend.entity.TotalTrendResponseDto;
import org.springframework.ai.embedding.Embedding;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

import com.sangchu.global.mapper.ResponseMapper;
import com.sangchu.global.response.BaseResponse;
import com.sangchu.global.util.statuscode.ApiStatus;
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
    private final EmbeddingService embeddingService;

    @GetMapping("/trend")
    public ResponseEntity<BaseResponse<List<TotalTrendResponseDto>>> getKeywordTrend(@RequestParam String keyword, @RequestParam int limit) throws IOException {
        return ResponseMapper.successOf(ApiStatus._OK, trendService.getTotalResults(keyword, limit), TrendController.class);
    }

    @GetMapping("/trendKeywords")
    public ResponseEntity<BaseResponse<List<KeywordInfo>>> getKeyWordInfo(@RequestParam String keyword, @RequestParam int limit, @RequestParam int numCandidates, @RequestParam String indexName) {
        Embedding keywordEmbedding = embeddingService.getEmbedding(keyword);

        return ResponseMapper.successOf(ApiStatus._OK, trendService.getTrendKeywords(limit, indexName, numCandidates, keywordEmbedding), TrendController.class);
    }
}
