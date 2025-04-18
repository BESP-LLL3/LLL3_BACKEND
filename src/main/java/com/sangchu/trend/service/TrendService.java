package com.sangchu.trend.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sangchu.elasticsearch.CosineSimilarity;
import com.sangchu.trend.entity.KeywordInfo;
import com.sangchu.trend.entity.KeywordSuggestionResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrendService {
	private final CosineSimilarity cosineSimilarity;

	public KeywordSuggestionResponseDto getKeywordList(String keyword, int limit) {

		Map<String, Integer> wordFrequency = cosineSimilarity.getWordFrequency(keyword);

		List<KeywordInfo> topKeywords = wordFrequency.entrySet().stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
			.limit(limit)
			.map(entry -> new KeywordInfo(entry.getKey(), entry.getValue()))
			.toList();
		return new KeywordSuggestionResponseDto(topKeywords);
	}
}
