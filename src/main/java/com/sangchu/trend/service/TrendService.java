package com.sangchu.trend.service;

import java.util.*;

import com.sangchu.elasticsearch.service.EsHelperService;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sangchu.elasticsearch.CosineSimilarity;
import com.sangchu.trend.entity.KeywordInfo;
import com.sangchu.trend.entity.TotalTrendResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendService {

    @Value("${spring.elasticsearch.index-name}")
    private String docsName;

    private final CosineSimilarity cosineSimilarity;
    private final EsHelperService esHelperService;

    public List<TotalTrendResponseDto> getTotalResults(String trendKeyword, int limit) {
        List<String> indexNames = esHelperService.getStoreSearchDocIndices()
                .orElseThrow(() -> new CustomException(ApiStatus._ES_INDEX_LIST_FETCH_FAIL));
        indexNames.sort(Comparator.naturalOrder());

        List<KeywordInfo> trendKeywords = getRecentKeywordInfos(trendKeyword, limit);

        Map<String, Map<String, Double>> indexToWordRelevanceMap = new HashMap<>();

        for (String indexName : indexNames) {
            try {
                Map<String, Double> wordRelevance = cosineSimilarity.getWordRelevance(trendKeyword, indexName);
                indexToWordRelevanceMap.put(indexName, wordRelevance);
            } catch (Exception e) {
                throw new CustomException(ApiStatus._ES_KEYWORD_COUNT_FAIL,
                        "인덱스 [" + indexName + "]의 WordFrequency 집계 실패");
            }
        }

        List<TotalTrendResponseDto> result = new ArrayList<>();

        for (KeywordInfo keywordInfo : trendKeywords) {
            String keyword = keywordInfo.getKeyword();
            double recentRelevance = keywordInfo.getRelevance();
            Map<String, Double> quarterRelevance = new HashMap<>();

            for (String indexName : indexNames) {
                String crtrYm = indexName.replace(docsName + "-", "");
                Double relevance = indexToWordRelevanceMap
                        .getOrDefault(indexName, Collections.emptyMap())
                        .getOrDefault(keyword, 0d);
                quarterRelevance.put(crtrYm, relevance);
            }

            result.add(new TotalTrendResponseDto(keyword, recentRelevance, quarterRelevance));
        }

        return result;
    }

    private List<KeywordInfo> getRecentKeywordInfos(String keyword, int limit) {

        String recentStoreSearchDocIndexName = docsName + "-" + esHelperService.getRecentCrtrYm();
        Map<String, Double> wordRelevance = cosineSimilarity.getWordRelevance(keyword, recentStoreSearchDocIndexName);

        return wordRelevance.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new KeywordInfo(entry.getKey(), entry.getValue()))
                .toList();
    }
}