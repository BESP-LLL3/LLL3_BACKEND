package com.sangchu.trend.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

        Map<String, Map<String, Double>> indexToWordRelevanceMap = new ConcurrentHashMap<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<?>> futures = new ArrayList<>();

        for (String indexName : indexNames) {
            futures.add(executor.submit(() -> {
                try {
                    log.info("IndexName: {}", indexName);
                    Map<String, Double> wordRelevance = cosineSimilarity.getWordRelevance(trendKeyword, indexName);
                    indexToWordRelevanceMap.put(indexName, wordRelevance);
                } catch (Exception e) {
                    throw new CustomException(ApiStatus._ES_KEYWORD_COUNT_FAIL,
                        "인덱스 [" + indexName + "]의 WordFrequency 집계 실패");
                }
            }));
        }

        // 모든 작업 완료 대기
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw new CustomException(ApiStatus._ES_KEYWORD_COUNT_FAIL);
            }
        }

        executor.shutdown();

        List<KeywordInfo> trendKeywords = getRecentKeywordInfos(indexToWordRelevanceMap, limit);
        List<TotalTrendResponseDto> result = new ArrayList<>();

        for (KeywordInfo keywordInfo : trendKeywords) {
            String keyword = keywordInfo.getKeyword();
            double recentRelevance = keywordInfo.getRelevance();

            // quarterRelevance를 정렬 순서를 유지하는 LinkedHashMap으로 선언
            Map<String, Double> quarterRelevance = new LinkedHashMap<>();

            // crtrYm 기준으로 정렬된 indexName 순서대로 relevance 삽입
            indexNames.stream()
                .sorted(Comparator.comparing(name -> name.replace(docsName + "-", "")))  // 분기 오름차순
                .forEach(indexName -> {
                    String crtrYm = indexName.replace(docsName + "-", "");
                    Double relevance = indexToWordRelevanceMap
                        .getOrDefault(indexName, Collections.emptyMap())
                        .getOrDefault(keyword, 0d);
                    quarterRelevance.put(crtrYm, relevance);
                });

            result.add(new TotalTrendResponseDto(keyword, recentRelevance, quarterRelevance));
        }

        return result;
    }


    private List<KeywordInfo> getRecentKeywordInfos(Map<String, Map<String, Double>> indexToWordRelevanceMap, int limit) {

        String recentStoreSearchDocIndexName = docsName + "-" + esHelperService.getRecentCrtrYm();

        Map<String, Double> wordRelevance = indexToWordRelevanceMap.get(recentStoreSearchDocIndexName);

        return wordRelevance.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .filter(entry -> entry.getKey().length() > 1)
            .limit(limit)
            .map(entry -> new KeywordInfo(entry.getKey(), entry.getValue()))
            .toList();
    }
}