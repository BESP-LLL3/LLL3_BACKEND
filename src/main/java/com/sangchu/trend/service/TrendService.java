package com.sangchu.trend.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sangchu.elasticsearch.CosineSimilarity;
import com.sangchu.embedding.service.EmbeddingHelperService;
import com.sangchu.trend.entity.KeywordInfo;
import com.sangchu.trend.entity.KeywordSuggestionResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrendService {
	private final EmbeddingHelperService embeddingHelperService;
	private final CosineSimilarity cosineSimilarity;

	public KeywordSuggestionResponseDto getKeywordList(String keyword, int limit) {
		/*
		 TODO: 주어진 keyword에 대해 다음 과정을 수행하여 유사 키워드를 추천한다.
		   - 1. keyword를 임베딩한다.
		   - 2. Elasticsearch의 문서 임베딩들과 cosine 유사도 비교를 수행한다.
		   3. 유사도가 기준치 이상인 문서를 필터링한다.
		   4. 필터링된 문서들의 형태소 분석 결과를 기반(문서 내에 포함)으로 각 키워드의 등장 빈도를 relevance로 설정한다.
		   5. relevance 정보를 포함하는 KeywordInfo 리스트를 구성하고,
			  이를 KeywordSuggestionResponseDto로 감싸서 반환한다.
		*/
		Map<String, Integer> wordFrequency = cosineSimilarity.getWordFrequency(keyword);
		// 3. 상위 단어 추출
		List<KeywordInfo> topKeywords = wordFrequency.entrySet().stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
			.limit(limit)
			.map(entry -> new KeywordInfo(entry.getKey(), entry.getValue()))
			.toList();
		return new KeywordSuggestionResponseDto(topKeywords);
	}
}
