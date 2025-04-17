package com.sangchu.batch.preprocess.service;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.patch.service.StoreHelperService;
import com.sangchu.batch.preprocess.entity.StoreSearchDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Service
public class PreprocessService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final StoreHelperService storeHelperService;
    private final EmbeddingService embeddingService;
    @Value("${spring.elk.index-name}")
    private String indexName;

    public void indexAll() {
        List<Store> stores = storeHelperService.getStoreAll();

        List<Embedding> embeddingList = stores.stream()
                .map(this::makeContextString)
                .map(embeddingService::getEmbedding)
                .toList();

        Map<String, StoreSearchDoc> docs = IntStream.range(0, stores.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> stores.get(i).getCrtrYm(),
                        i -> new StoreSearchDoc(stores.get(i), embeddingList.get(i))
                ));

        List<IndexQuery> queries = docs.entrySet().stream()
                .map(entry -> new IndexQueryBuilder()
                        .withIndex(indexName + "-" + entry.getKey())
                        .withObject(entry.getValue())
                        .build())
                .toList();

        elasticsearchOperations.bulkIndex(queries, StoreSearchDoc.class);
    }

    private String makeContextString(Store store) {
        String storeNm = store.getStoreNm();
        String MidCatNm = store.getMidCatNm();
        String SmallCatNm = store.getSmallCatNm();

        String contextString = storeNm + MidCatNm + SmallCatNm;

        return contextString; // 예시
    }
}
