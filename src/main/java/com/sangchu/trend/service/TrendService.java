package com.sangchu.trend.service;

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

        List<KeywordInfo> trendKeywords = getKeywordInfos(30, recentStoreSearchDocIndexName, 120, keywordEmbedding).stream()
                .filter(k -> k.getKeyword().length() > 1) // 글자 수 1인 키워드 제거
                .sorted((a, b) -> Double.compare(b.getRelevance(), a.getRelevance())) // 내림차순 정렬
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(8);

        List<Callable<Map.Entry<String, List<KeywordInfo>>>> tasks = indexNames.stream()
                .map(indexName -> (Callable<Map.Entry<String, List<KeywordInfo>>>) () -> {
                    try {
                        log.info("Getting trend results for {}", indexName);
                        List<KeywordInfo> wordRelevance = getKeywordInfos(30, indexName, 120, keywordEmbedding);
                        return Map.entry(indexName, wordRelevance);
                    } catch (Exception e) {
                        log.warn("index 실패: {}", indexName);
                        return Map.entry(indexName, Collections.emptyList());
                    }
                }).toList();

        Map<String, List<KeywordInfo>> indexToWordRelevanceMap = new HashMap<>();
        try {
            List<Future<Map.Entry<String, List<KeywordInfo>>>> futures = executor.invokeAll(tasks);
            for (Future<Map.Entry<String, List<KeywordInfo>>> future : futures) {
                Map.Entry<String, List<KeywordInfo>> entry = future.get();
                indexToWordRelevanceMap.put(entry.getKey(), entry.getValue());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomException(ApiStatus._ES_KEYWORD_COUNT_FAIL, "인덱스 relevance 수집 중 오류 발생");
        } finally {
            executor.shutdown(); // 자원 반납
        }

        List<TotalTrendResponseDto> result = new ArrayList<>();

        for (KeywordInfo keywordInfo : trendKeywords) {
            String keyword = keywordInfo.getKeyword();
            double recentRelevance = keywordInfo.getRelevance();
            Map<String, Double> quarterRelevance = new HashMap<>();

            for (String indexName : indexNames) {
                String crtrYm = indexName.replace(docsName + "-", "");

                List<KeywordInfo> keywordInfos = indexToWordRelevanceMap.getOrDefault(indexName, Collections.emptyList());

                double relevance = keywordInfos.stream()
                        .filter(info -> info.getKeyword().equals(keyword))
                        .mapToDouble(KeywordInfo::getRelevance)
                        .sum();

                quarterRelevance.put(crtrYm, quarterRelevance.getOrDefault(crtrYm, 0.0) + relevance);
            }

            Map<String, Double> sortedByKey = new TreeMap<>(quarterRelevance);

            result.add(new TotalTrendResponseDto(keyword, recentRelevance, sortedByKey));
        }

        return result;
    }

    public List<KeywordInfo> getKeywordInfos(int k, String indexName, int numCandidates, Embedding keywordEmbedding) {
        try {
            SearchResponse<StoreSearchDoc> response = esHelperService.searchKnn(indexName, keywordEmbedding.getOutput(), k, numCandidates);

            Map<String, Double> wordRelevance = new HashMap<>();

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
                    wordRelevance.put(token, wordRelevance.getOrDefault(token, 0d) + score);
                }
            }

            return wordRelevance.entrySet().stream()
                    .map(entry -> new KeywordInfo(entry.getKey(), entry.getValue()))
                    .toList();
        } catch (Exception e) {
            log.error("Error during getKeywordInfos for index [{}]: {}", indexName, e.getMessage(), e);
            throw new CustomException(ApiStatus._BAD_REQUEST);
        }
    }

//    private List<KeywordInfo> getRecentKeywordInfos(String keyword, int limit) {
//
//        String recentStoreSearchDocIndexName = docsName + "-" + esHelperService.getRecentCrtrYm();
//        Map<String, Double> wordRelevance = cosineSimilarity.getWordRelevance(keyword, recentStoreSearchDocIndexName);
//
//        return wordRelevance.entrySet()
//                .stream()
//                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
//                .limit(limit)
//                .map(entry -> new KeywordInfo(entry.getKey(), entry.getValue()))
//                .toList();
//    }
}