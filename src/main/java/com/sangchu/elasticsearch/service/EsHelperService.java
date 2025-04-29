package com.sangchu.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.sangchu.elasticsearch.entity.RecentIndexingDoc;
import com.sangchu.elasticsearch.entity.StoreSearchDoc;
import com.sangchu.elasticsearch.repository.RecentIndexingDocRepository;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.io.IOException;
import java.util.*;
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
        Query query = new CriteriaQuery(new Criteria());

        SearchHits<StoreSearchDoc> searchHits = elasticsearchOperations.search(
                query,
                StoreSearchDoc.class,
                IndexCoordinates.of(indexName)
        );

        return searchHits.get().map(SearchHit::getContent).toList();
    }

    public SearchResponse<StoreSearchDoc> searchKnn(String indexName, float[] queryVector, int k, int numCandidates) throws IOException {
        List<Float> queryVectorList = new ArrayList<>();
        for (float vector : queryVector) {
            queryVectorList.add(vector);
        }
        SearchRequest request = SearchRequest.of(s -> s
                .index(indexName)
                .knn(knn -> knn
                        .field("vector")  // dense_vector 필드명
                        .queryVector(queryVectorList)  // float[] 형식
                        .k(k)
                        .numCandidates(numCandidates)
                        .similarity(0.3F)
                )
        );

        return elasticsearchClient.search(request, StoreSearchDoc.class);
    }
}
