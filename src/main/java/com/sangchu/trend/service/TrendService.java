package com.sangchu.trend.service;

import java.io.IOException;
import java.util.*;
import java.util.stream.StreamSupport;

import com.sangchu.elasticsearch.entity.StoreSearchDoc;
import com.sangchu.elasticsearch.service.EsHelperService;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import org.hibernate.query.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import com.sangchu.elasticsearch.CosineSimilarity;
import com.sangchu.trend.entity.KeywordInfo;
import com.sangchu.trend.entity.KeywordSuggestionResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrendService {
	private final CosineSimilarity cosineSimilarity;
	private final ElasticsearchOperations elasticsearchOperations;
	private final EsHelperService esHelperService;

	public KeywordSuggestionResponseDto getKeywordList(String keyword, int limit) {

		Map<String, Integer> wordFrequency = cosineSimilarity.getWordFrequency(keyword);

		List<KeywordInfo> topKeywords = wordFrequency.entrySet().stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
			.limit(limit)
			.map(entry -> new KeywordInfo(entry.getKey(), entry.getValue()))
			.toList();
		return new KeywordSuggestionResponseDto(topKeywords);
	}

	public List<Map<String, Object>> getQuarterlyWordFrequencyAcrossIndices(String keyword) throws IOException {
        List<String> indexNames = esHelperService.getStoreSearchDocIndices(); // store_search_doc-YYYYMM 형식

        List<Map<String, Object>> result = new ArrayList<>();

        for (String indexName : indexNames) {
            try {
                SearchHits<StoreSearchDoc> searchHits = elasticsearchOperations.search(
                        Query.findAll(),
                        StoreSearchDoc.class,
                        IndexCoordinates.of(indexName)
                );

                long count = searchHits.stream()
                        .map(SearchHit::getContent)
                        .map(StoreSearchDoc::getTokens)
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .filter(token -> token.equals(keyword))
                        .count();

                String crtrYm = indexName.replace("store_search_doc-", "");

                Map<String, Object> item = new HashMap<>();
                item.put("crtrYm", crtrYm);
                item.put("count", count);

                result.add(item);

            } catch (Exception e) {
                throw new CustomException(ApiStatus._ES_KEYWORD_COUNT_FAIL,
                        "인덱스 [" + indexName + "]에서 키워드 카운트 중 예외 발생");
            }
        }
        return result;
    }
}

