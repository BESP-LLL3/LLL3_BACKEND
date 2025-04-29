package com.sangchu.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.sangchu.elasticsearch.entity.RecentIndexingDoc;
import com.sangchu.elasticsearch.entity.StoreSearchDoc;
import com.sangchu.elasticsearch.repository.RecentIndexingDocRepository;
import com.sangchu.embedding.service.EmbeddingService;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.query.NativeQuery;
import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchScrollHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EsHelperService {
    
    @Value("${spring.elasticsearch.index-name}")
    private String docsName;
    
    private final RecentIndexingDocRepository recentIndexingDocRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchClient elasticsearchClient;

    public void indexRecentCrtrYm(String crtrYm) {
        try {
            RecentIndexingDoc doc = RecentIndexingDoc.builder()
                    .id("recent_crtr_ym")
                    .crtrYm(crtrYm)
                    .build();

            recentIndexingDocRepository.save(doc);
        } catch (Exception e) {
            throw new CustomException(ApiStatus._ES_CRTRYM_INDEXING_FAIL , "최근 분기 인덱싱 중 예외 발생 - crtrYm : " + crtrYm);
        }
    }

    public String getRecentCrtrYm() {
        String recentCrtrYm = recentIndexingDocRepository.findById("recent_crtr_ym")
                .orElseThrow(() -> new CustomException(ApiStatus._RECENT_CRTRYM_NOT_FOUND))
                .getCrtrYm();
        if (recentCrtrYm == null) {
            throw new CustomException(ApiStatus._RECENT_CRTRYM_NOT_FOUND);
        }
        return recentCrtrYm;
    }

    public Optional<List<String>> getStoreSearchDocIndices(){
        try {
            IndicesResponse response = elasticsearchClient.cat().indices();

            return Optional.of(response.valueBody().stream()
                    .map(IndicesRecord::index)
                    .filter(Objects::nonNull)
                    .filter(name -> name.startsWith(docsName + "-"))
                    .collect(Collectors.toList()));

        } catch (Exception e) {
            throw new CustomException(ApiStatus._ES_INDEX_LIST_FETCH_FAIL);
        }
    }

    // public List<StoreSearchDoc> findDocsByName(String indexName) {
    //     List<StoreSearchDoc> results = new ArrayList<>();
    //     List<FieldValue> searchAfter = null; // search_after는 List<FieldValue> 타입이어야 함
    //
    //     while (true) {
    //         // SearchRequest 빌드
    //         SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
    //             .index(indexName)
    //             .size(8000) // 한 페이지 당 가져올 문서 수
    //             .query(q -> q.matchAll(m -> m)) // 전체 조회, 필요한 경우 다른 query로 대체
    //             .sort(s -> s
    //                 .field(f -> f
    //                     .field("storeId.keyword") // keyword 필드로 정렬해야 함
    //                     .order(SortOrder.Asc)
    //                 )
    //             );
    //
    //         // search_after 값 설정 (첫 요청은 null이므로 생략)
    //         if (searchAfter != null) {
    //             requestBuilder.searchAfter(searchAfter);
    //         }
    //         log.info("while true loop - searchAfter : {}", searchAfter);
    //
    //         // Elasticsearch 검색 요청 실행
    //         try {
    //             SearchResponse<StoreSearchDoc> response = elasticsearchClient.search(requestBuilder.build(), StoreSearchDoc.class);
    //             List<Hit<StoreSearchDoc>> hits = response.hits().hits();
    //
    //             if (hits.isEmpty()) break; // 더 이상 데이터가 없으면 종료
    //
    //             // 결과 추가
    //             hits.forEach(hit -> results.add(hit.source()));
    //
    //             // 마지막 문서의 sort 값 추출 및 search_after 설정
    //             searchAfter = hits.get(hits.size() - 1).sort();
    //             log.info("searchAfter set: {}", searchAfter);
    //
    //         } catch (Exception e) {
    //             log.error("Error during Elasticsearch search: {}", e.getMessage());
    //             throw new CustomException(ApiStatus._ES_READ_FAIL);
    //         }
    //     }
    //     return results;
    // }

    public List<StoreSearchDoc> findDocsByName(String indexName, Embedding queryVector, int k) {
        List<StoreSearchDoc> results = new ArrayList<>();

        try {
            // float[] → List<Float> 변환
            float[] vector = queryVector.getOutput();
            List<Float> vectorList = new ArrayList<>();
            for (float v : vector) vectorList.add(v);

            // SearchRequest 생성
            SearchRequest request = new SearchRequest.Builder()
                .index(indexName)
                .timeout("360s")
                .size(k) // top-k 문서만 가져오기
                .query(q -> q
                    .scriptScore(ss -> ss
                        .query(inner -> inner.matchAll(m -> m)) // 전체 문서에 대해 적용
                        .script(script -> script
                            .source("cosineSimilarity(params.queryVector, 'vector') + 1.0") // 유사도 계산
                            .params("queryVector", JsonData.of(vectorList))
                        )
                    )
                )
                .build();
            log.info("request Builder start : {}", indexName);
            // 검색 실행
            SearchResponse<StoreSearchDoc> response = elasticsearchClient.search(request, StoreSearchDoc.class);
            for (Hit<StoreSearchDoc> hit : response.hits().hits()) {
                results.add(hit.source());
            }
            log.info("request Builder end");

        } catch (Exception e) {
            log.error("Error during script_score search: {}", e.getMessage());
            throw new CustomException(ApiStatus._ES_READ_FAIL);
        }

        return results;
    }

}
