package com.sangchu.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    public List<StoreSearchDoc> findDocsByName(String indexName) {
        List<StoreSearchDoc> results = new ArrayList<>();
        Object[] searchAfter = null; // search_after는 Object[] 타입이어야 함

        do {
            Query query = new CriteriaQuery(new Criteria());
            query.setPageable(PageRequest.of(0, 8000)); // page=0 고정, size만 8000
            query.addSort(Sort.by(Sort.Order.asc("_id"))); // 정렬 기준 필수

            if (searchAfter != null) {
                query.setSearchAfter(Arrays.asList(searchAfter));
            }

            SearchHits<StoreSearchDoc> searchHits = elasticsearchOperations.search(
                query,
                StoreSearchDoc.class,
                IndexCoordinates.of(indexName)
            );

            searchHits.getSearchHits().forEach(hit -> results.add(hit.getContent()));

            if (!searchHits.getSearchHits().isEmpty()) {
                searchAfter = searchHits.getSearchHits()
                    .getLast()
                    .getSortValues()
                    .toArray(); // SortValues를 그대로 Object[]로 변환
            } else {
                searchAfter = null;
            }
        } while (searchAfter != null);

        return results;
    }

}
