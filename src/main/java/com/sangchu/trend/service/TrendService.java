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

    public List<TotalTrendResponseDto> getTotalResults(String trendKeyword, int limit) {
        List<String> indexNames = esHelperService.getStoreSearchDocIndices()
                .orElseThrow(() -> new CustomException(ApiStatus._ES_INDEX_LIST_FETCH_FAIL));
        indexNames.sort(Comparator.naturalOrder());
        String recentStoreSearchDocIndexName = docsName + "-" + esHelperService.getRecentCrtrYm();
        Embedding keywordEmbedding = embeddingService.getEmbedding(trendKeyword);

        List<KeywordInfo> trendKeywords = getTrendKeywords(30, recentStoreSearchDocIndexName, 120, keywordEmbedding).stream()
                .filter(k -> k.getKeyword().length() > 1) // 글자 수 1인 키워드 제거
                .sorted((a, b) -> Double.compare(b.getRelevance(), a.getRelevance())) // 내림차순 정렬
                .toList();

        Map<String, Map<String, Double>> analyzeTrends = analyzeTrends(trendKeywords, keywordEmbedding.getOutput(), indexNames);

        List<TotalTrendResponseDto> results = trendKeywords.stream()
                .map(keywordInfo -> {
                    String keyword = keywordInfo.getKeyword();

                    // 전체 분기별 점수 맵
                    Map<String, Double> quarterRelevance = analyzeTrends.getOrDefault(keyword, Collections.emptyMap());

                    // 최근 분기의 relevance 점수
                    Double recentScore = quarterRelevance.get(esHelperService.getRecentCrtrYm());

                    return new TotalTrendResponseDto(keyword, recentScore, quarterRelevance);
                })
                .sorted(Comparator.comparingDouble(TotalTrendResponseDto::getRecentCrtrYmRelevance).reversed())
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
                    scoreSum.put(token, scoreSum.getOrDefault(token, 0d) + score);
                    frequency.put(token, frequency.getOrDefault(token, 0) + 1);
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
        // ConcurrentMap으로 병렬 환경에서 안전하게 결과 수집
        Map<String, Map<String, Double>> trendKeywordScores = trendKeywords.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        KeywordInfo::getKeyword, // 키: 키워드 문자열
                        keywordInfo -> {
                            Map<String, Double> scoreMap = new HashMap<>();
                            for (String indexName : indexList) {
                                try {
                                    SearchResponse<StoreSearchDoc> response =
                                            esHelperService.searchKnnWithTokenFilter(
                                                    indexName,
                                                    keywordVector,
                                                    30,      // k
                                                    120,      // num_candidates (속도 개선 가능)
                                                    List.of(keywordInfo.getKeyword())
                                            );

                                    // 각 문서의 score 합산
                                    for (Hit<StoreSearchDoc> hit : response.hits().hits()) {
                                        Double hitScore = hit.score();
                                        if (hitScore != null && hitScore > 0.0) {
                                            StoreSearchDoc source = hit.source();
                                            List<String> tokens = source.getTokens();

                                            if (tokens != null) {
                                                // 각 토큰마다 점수를 누적
                                                for (String token : tokens) {
                                                    // 만약 현재 토큰이 우리가 찾고 있는 키워드라면 점수 추가
                                                    if (token.equals(keywordInfo.getKeyword())) {
                                                        scoreMap.put(indexName,
                                                                scoreMap.getOrDefault(indexName, 0.0) + hitScore);
                                                    }
                                                }
                                            }
                                        }
                                    }

                                } catch (IOException e) {
                                    log.warn("KNN 검색 실패: keyword={}, indexName={}, error={}",
                                            keywordInfo.getKeyword(), indexName, e.getMessage());
                                    // 실패한 인덱스에 대해서는 0점 처리
                                    scoreMap.put(indexName, 0.0);
                                }
                            }
                            return scoreMap;
                        }
                ));

        return trendKeywordScores;
    }
}