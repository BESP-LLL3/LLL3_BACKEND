package com.sangchu.batch.preprocess.job;

import com.sangchu.embedding.entity.StoreSearchDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreWriter implements ItemWriter<List<IndexQuery>> {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void write(Chunk<? extends List<IndexQuery>> chunk) {
        List<IndexQuery> items = chunk.getItems().stream()
                .flatMap(List::stream)
                .toList();
        
        try {
            elasticsearchOperations.bulkIndex(items, StoreSearchDoc.class);
        } catch (Exception e) {
            log.error("Index 생성 중 에러 발생", e);
            throw e;
        }
    }
}
