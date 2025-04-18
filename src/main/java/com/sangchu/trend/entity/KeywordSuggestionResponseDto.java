package com.sangchu.trend.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public class KeywordSuggestionResponseDto {
	private List<KeywordInfo> trendKeywords;
}