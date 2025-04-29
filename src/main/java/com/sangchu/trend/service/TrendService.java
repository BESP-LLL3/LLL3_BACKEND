package com.sangchu.trend.service;

import java.util.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
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

        List<KeywordInfo> trendKeywords = getKeywordInfos(trendKeyword, limit, recentStoreSearchDocIndexName);

        Map<String, List<KeywordInfo>> indexToWordRelevanceMap = new HashMap<>();

        for (String indexName : indexNames) {
            try {
                List<KeywordInfo> wordRelevance = getKeywordInfos(trendKeyword, 30, indexName);
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

                List<KeywordInfo> keywordInfos = indexToWordRelevanceMap.getOrDefault(indexName, Collections.emptyList());

                double relevance = keywordInfos.stream()
                        .filter(info -> info.getKeyword().equals(keyword))
                        .map(KeywordInfo::getRelevance)
                        .findFirst()
                        .orElse(0.0);

                quarterRelevance.put(crtrYm, relevance);
            }

            result.add(new TotalTrendResponseDto(keyword, recentRelevance, quarterRelevance));
        }

        return result;
    }

    public List<KeywordInfo> getKeywordInfos(String keyword, int limit, String indexName) {
        Embedding keywordEmbedding = embeddingService.getEmbedding(keyword);

        try {
            SearchResponse<StoreSearchDoc> response = esHelperService.searchKnn(indexName, keywordEmbedding.getOutput(), limit, 100);

            Map<String, Double> wordRelevance = new HashMap<>();

            for (Hit<StoreSearchDoc> hit : response.hits().hits()) {
                StoreSearchDoc source = hit.source();
                Double score = hit.score();
                List<String> tokens = source.getTokens();

                for (String token : tokens) {
                    wordRelevance.put(token, wordRelevance.getOrDefault(token, 0d) + score);
                }
            }


            return wordRelevance.entrySet().stream()
                    .map(entry -> new KeywordInfo(entry.getKey(), entry.getValue()))
                    .sorted((a, b) -> Double.compare(b.getRelevance(), a.getRelevance())) // 내림차순 정렬
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            log.error(e.getMessage());
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