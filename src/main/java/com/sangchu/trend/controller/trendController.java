package com.sangchu.trend.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(
        origins = "http://localhost:3000",
        allowCredentials = "true"
) // React 포트
@RestController
@RequestMapping("/api")
public class trendController {

    @GetMapping("/keyword-trend")
    public Map<String, Object> getKeywordTrend(@RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        response.put("keyword", keyword);

        List<Integer> trendData = Arrays.asList(
                16, 361, 1018, 2025, 3192, 4673, 5200, 4673,
                4913, 3000, 2500, 2000, 4000, 3000, 3500,
                5000, 1500, 2000, 3000, 4100
        );

        response.put("trendData", trendData);
        System.out.println("response: " + response);
        return response;
    }
}
