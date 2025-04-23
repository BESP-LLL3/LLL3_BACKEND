package com.sangchu.batch.preprocess.job;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.elasticsearch.MorphologicalAnalysis;
import com.sangchu.embedding.entity.StoreSearchDoc;
import com.sangchu.embedding.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchItemProcessor implements ItemProcessor<List<Store>, List<IndexQuery>> {

    private final EmbeddingService embeddingService;
    private final MorphologicalAnalysis morphologicalAnalysis;

    @Override
    public List<IndexQuery> process(List<Store> stores) {
        List<String> contexts = stores.stream()
                .map(this::makeContextString)
                .toList();

        List<Embedding> embeddings = embeddingService.getBatchEmbeddings(contexts);

        try {
            List<IndexQuery> result = IntStream.range(0, stores.size())
                    .mapToObj(i -> {
                        Store store = stores.get(i);
                        String crtrYm = store.getCrtrYm();

                        List<String> tokens = morphologicalAnalysis.extractNouns(store.getStoreNm());
                        StoreSearchDoc doc = new StoreSearchDoc(store, embeddings.get(i), tokens);

                        return new IndexQueryBuilder()
                                .withId(doc.getStoreId())
                                .withIndex("store_search_doc-" + crtrYm)
                                .withObject(doc)
                                .build();
                    })
                    .toList();

            log.info("IndexQuery 생성 성공! {}", result.size());
            return result;

        } catch (Exception e) {
            log.error("IndexQuery 생성 중 에러 발생", e);
            throw e;
        }
    }

    private String makeContextString(Store store) {
        return store.getStoreNm() + store.getMidCatNm() + store.getSmallCatNm();
    }
}
