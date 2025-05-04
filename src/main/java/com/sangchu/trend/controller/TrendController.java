package com.sangchu.trend.controller;

import com.sangchu.trend.entity.TotalTrendResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

import com.sangchu.global.mapper.ResponseMapper;
import com.sangchu.global.response.BaseResponse;
import com.sangchu.global.util.statuscode.ApiStatus;
import com.sangchu.trend.service.TrendService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TrendController {
    private final TrendService trendService;

    @GetMapping("/trend")
    public ResponseEntity<BaseResponse<List<TotalTrendResponseDto>>> getKeywordTrend(@RequestParam String keyword, @RequestParam int limit) throws IOException {
        return ResponseMapper.successOf(ApiStatus._OK, trendService.getTotalResults(keyword, limit), TrendController.class);
    }
}
