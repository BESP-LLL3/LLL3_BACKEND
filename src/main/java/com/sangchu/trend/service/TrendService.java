package com.sangchu.trend.service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.Pair;
import com.sangchu.elasticsearch.entity.StoreSearchDoc;
import com.sangchu.elasticsearch.service.EsHelperService;
import com.sangchu.embedding.service.EmbeddingService;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;

import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    private final EmbeddingService embeddingService;
    private final EsHelperService esHelperService;

    private final int k = 40;
    private final int numCandidates = 80;

    public List<TotalTrendResponseDto> getTotalResults(String trendKeyword, int limit) {
        List<String> indexNames = esHelperService.getStoreSearchDocIndices()
                .orElseThrow(() -> new CustomException(ApiStatus._ES_INDEX_LIST_FETCH_FAIL));
        indexNames.sort(Comparator.naturalOrder());

        String recentStoreSearchDocIndexName = docsName + "-" + esHelperService.getRecentCrtrYm();
        Embedding keywordEmbedding = embeddingService.getEmbedding(trendKeyword);

        List<KeywordInfo> trendKeywords = getTrendKeywords(k, recentStoreSearchDocIndexName, numCandidates, keywordEmbedding).stream()
                .filter(k -> k.getKeyword().length() > 1) // 글자 수 1인 키워드 제거
                .sorted((a, b) -> Double.compare(b.getRelevance(), a.getRelevance())) // 내림차순 정렬
                .toList();

        Map<String, Map<String, Double>> analyzeTrends = analyzeTrends(trendKeywords, keywordEmbedding.getOutput(), indexNames);

        List<TotalTrendResponseDto> results = trendKeywords.stream()
                .map(keywordInfo -> {
                    String keyword = keywordInfo.getKeyword();

                    // 전체 분기별 점수 맵
                    Map<String, Double> quarterRelevance = analyzeTrends.getOrDefault(keyword, Collections.emptyMap());

                    Map<String, Double> sortedQuarterRelevance = quarterRelevance.entrySet().stream()
                            .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey()))
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (a, b) -> b,
                                    LinkedHashMap::new
                            ));

                    // 최근 분기의 relevance 점수
                    Double recentScore = keywordInfo.getRelevance();

                    return new TotalTrendResponseDto(keyword, recentScore, sortedQuarterRelevance);
                })
                .sorted(Comparator.comparingDouble(TotalTrendResponseDto::getRecentCrtrYmRelevance).reversed())
                .limit(limit)
                .toList();

        return results;
    }

    public List<KeywordInfo> getTrendKeywords(int k, String indexName, int numCandidates, Embedding keywordEmbedding) {
        try {
            SearchResponse<StoreSearchDoc> response = esHelperService.searchKnn(indexName, keywordEmbedding.getOutput(), k, numCandidates);

            Map<String, Double> scoreSum = new HashMap<>();
            Map<String, Integer> frequency = new HashMap<>();

            for (Hit<StoreSearchDoc> hit : response.hits().hits()) {
                Double score = hit.score();
                if (score == null || score < 0.1) {
                    continue;
                }

                StoreSearchDoc source = hit.source();
                List<String> tokens = source.getTokens();
                if (tokens == null || tokens.isEmpty()) {
                    continue;
                }

                for (String token : tokens) {
                    if (tokens.contains(token)) {
                        scoreSum.put(token, scoreSum.getOrDefault(token, 0d) + score);
                        frequency.put(token, frequency.getOrDefault(token, 0) + 1);
                    }
                }
            }

            Map<String, Double> finalScore = new HashMap<>();
            for (String token : scoreSum.keySet()) {
                int freq = frequency.getOrDefault(token, 0);

                double score = scoreSum.get(token);
                double weightedScore = score * Math.log(freq + 1);
                finalScore.put(token, weightedScore);
            }

            return finalScore.entrySet().stream()
                    .filter(entry -> entry.getKey().length() > 1) // 글자 수 1인 키워드 제거
                    .map(entry -> new KeywordInfo(entry.getKey(), entry.getValue()))
                    .sorted((a, b) -> Double.compare(b.getRelevance(), a.getRelevance())) // 내림차순 정렬
                    .limit(30)
                    .toList();

        } catch (Exception e) {
            throw new CustomException(ApiStatus._BAD_REQUEST);
        }
    }

    public Map<String, Map<String, Double>> analyzeTrends(
            List<KeywordInfo> trendKeywords,
            float[] keywordVector,
            List<String> indexList
    ) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Map<String, Map<String, Double>> finalResult = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = indexList.stream()
                .map(indexName -> CompletableFuture.runAsync(() -> {
                    try {
                        SearchResponse<StoreSearchDoc> response = esHelperService.searchKnn(
                                indexName,
                                keywordVector,
                                k,
                                numCandidates
                        );

                        List<Hit<StoreSearchDoc>> hits = response.hits().hits();

                        for (KeywordInfo keywordInfo : trendKeywords) {
                            double totalScore = hits.stream()
                                    .filter(hit -> hit.score() != null && hit.score() > 0.0)
                                    .filter(hit -> {
                                        StoreSearchDoc source = hit.source();
                                        List<String> tokens = source.getTokens();
                                        return tokens != null && tokens.contains(keywordInfo.getKeyword());
                                    })
                                    .mapToDouble(Hit::score)
                                    .sum();

                            finalResult
                                    .computeIfAbsent(keywordInfo.getKeyword(), k -> new ConcurrentHashMap<>())
                                    .put(indexName, totalScore);
                        }

                    } catch (IOException e) {
                        log.warn("KNN 검색 실패: indexName={}, error={}", indexName, e.getMessage());
                        for (KeywordInfo keywordInfo : trendKeywords) {
                            finalResult
                                    .computeIfAbsent(keywordInfo.getKeyword(), k -> new ConcurrentHashMap<>())
                                    .put(indexName, 0.0);
                        }
                    }
                }, executor))
                .toList();

        // 모든 병렬 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        return finalResult;
    }
}