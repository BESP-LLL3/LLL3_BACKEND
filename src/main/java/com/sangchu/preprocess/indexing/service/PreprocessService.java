package com.sangchu.preprocess.indexing.service;

import com.sangchu.preprocess.etl.entity.Store;
import com.sangchu.preprocess.etl.service.StoreHelperService;
import com.sangchu.elasticsearch.MorphologicalAnalysis;
import com.sangchu.elasticsearch.entity.StoreSearchDoc;

import com.sangchu.embedding.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Service
public class PreprocessService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final StoreHelperService storeHelperService;
    private final EmbeddingService embeddingService;
    private final MorphologicalAnalysis morphologicalAnalysis;
    
    @Value("${spring.elasticsearch.index-name}")
    private String indexName;

    public void indexAll() {
        List<Store> stores = storeHelperService.getStoreWithoutFranchise(0L, 2000);

        List<String> contexts = stores.stream()
                .map(this::makeContextString)
                .toList();

        List<Embedding> embeddingList = embeddingService.getBatchEmbeddings(contexts);

        List<IndexQuery> queries = IntStream.range(0, stores.size())
                .mapToObj(i -> {
                    Store store = stores.get(i);
                    String crtrYm = store.getCrtrYm();

                    List<String> tokens = morphologicalAnalysis.extractNouns(store.getStoreNm());
                    StoreSearchDoc doc = new StoreSearchDoc(store, embeddingList.get(i), tokens);

                    return new IndexQueryBuilder()
                        .withIndex(indexName + "-" + crtrYm)
                        .withObject(doc)
                        .build();
                })
                .toList();

        elasticsearchOperations.bulkIndex(queries, StoreSearchDoc.class);
    }

    private String makeContextString(Store store) {
        String storeNm = store.getStoreNm();
        String largeCatNm = store.getLargeCatNm();
        String midCatNm = store.getMidCatNm();
        String smallCatNm = store.getSmallCatNm();

        StringBuilder sb = new StringBuilder();

        // 중요도에 따른 강조 반복
        sb.append(repeatEmphasis(storeNm, 15));
        sb.append(repeatEmphasis(smallCatNm, 10));
        sb.append(repeatEmphasis(midCatNm, 5));
        sb.append(repeatEmphasis(largeCatNm, 1));

        // 문장 형태로 정리
        String contextString = String.format(
                "%s는 %s 매장입니다. 참고로, 이 매장은 %s 업종에 속하며 %s 업종의 일종입니다.",
                storeNm,
                smallCatNm,
                midCatNm,
                largeCatNm
        );

        // 강조 문구 + 문장 결합
        return sb.toString().trim() + ". " + contextString;
    }

    private String repeatEmphasis(String word, int count) {
        return word.concat(" ").repeat(count);
    }
}
