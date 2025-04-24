package com.sangchu.preprocess.indexing.job;

import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import com.sangchu.preprocess.etl.entity.Store;
import com.sangchu.elasticsearch.MorphologicalAnalysis;
import com.sangchu.elasticsearch.entity.StoreSearchDoc;
import com.sangchu.embedding.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLEngineResult;
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
            throw new CustomException(ApiStatus._ES_INDEX_QUERY_CREATE_FAIL);
        }
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
