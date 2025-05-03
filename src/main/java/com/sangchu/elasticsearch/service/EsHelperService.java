package com.sangchu.elasticsearch.service;

import static com.sangchu.elasticsearch.CosineSimilarity.*;

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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
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
            throw new CustomException(ApiStatus._ES_CRTRYM_INDEXING_FAIL , "최근 분기 => " + crtrYm);
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
                .size(k)
                .query(q -> q
                    .scriptScore(ss -> ss
                        .query(inner -> inner
                            .bool(b -> b
                                .must(m -> m.matchAll(mq -> mq)) // 전체 문서 대상
                                .mustNot(mn -> mn.wildcard(wc -> wc.field("storeNm.keyword").value("*점")))
                                .mustNot(mn -> mn.wildcard(wc -> wc.field("storeNm.keyword").value("*번지")))
                            )
                        )
                        .script(script -> script
                            .source("cosineSimilarity(params.queryVector, 'vector') + 1.0")
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
