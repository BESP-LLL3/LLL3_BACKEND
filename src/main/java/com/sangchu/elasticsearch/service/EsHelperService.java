package com.sangchu.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import com.sangchu.elasticsearch.entity.RecentIndexingDoc;
import com.sangchu.elasticsearch.entity.StoreSearchDoc;
import com.sangchu.elasticsearch.repository.RecentIndexingDocRepository;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EsHelperService {
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

    public List<StoreSearchDoc> findRecentCrtrYmDocs() {
        // 1. 최근 분기 crtrYm 가져오기 (ID가 1인 단일 문서)
        String crtrYm = recentIndexingDocRepository.findById("1")
                .orElseThrow(() -> new CustomException(ApiStatus._RECENT_CRTRYM_NOT_FOUND))
                .getCrtrYm();

        // 2. 동적 인덱스 지정
        String indexName = "store_search_doc-" + crtrYm;

        Query query = new CriteriaQuery(new Criteria());

        SearchHits<StoreSearchDoc> searchHits = elasticsearchOperations.search(
                query,
                StoreSearchDoc.class,
                IndexCoordinates.of(indexName)
        );

        return searchHits.get().map(SearchHit::getContent).toList();
    }

    public List<String> getStoreSearchDocIndices() throws IOException {
        try {
            IndicesResponse response = elasticsearchClient.cat().indices();

            return response.valueBody().stream()
                    .map(IndicesRecord::index)
                    .filter(Objects::nonNull)
                    .filter(name -> name.startsWith("store_search_doc-"))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new CustomException(ApiStatus._ES_INDEX_LIST_FETCH_FAIL, "Elasticsearch 인덱스 목록 조회 중 예외 발생");
        }
    }
}
