package com.sangchu.trend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class TotalTrendResponseDto {
    private String keyword;
    private Double recentCrtrYmRelevance;
    private Map<String, Double> quarterRevelance;
}
