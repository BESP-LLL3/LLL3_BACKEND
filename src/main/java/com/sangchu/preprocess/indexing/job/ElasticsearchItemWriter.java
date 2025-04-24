package com.sangchu.preprocess.indexing.job;

import com.sangchu.elasticsearch.entity.StoreSearchDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchItemWriter implements ItemWriter<List<IndexQuery>> {

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
            Set<String> indexNames = items.stream()
                    .map(IndexQuery::getIndexName)
                    .collect(Collectors.toSet());

            for (String indexName : indexNames) {
                IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(indexName));
                if (!indexOps.exists()) {
                    // 인덱스 설정: 샤드 & 리플리카 수만 지정
                    Map<String, Object> settings = Map.of(
                            "index.number_of_shards", 3,
                            "index.number_of_replicas", 1
                    );

                    // 인덱스 생성 + 매핑 설정
                    indexOps.create(settings);

                    log.info("✅ 인덱스 '{}' (샤드/리플리카) 생성 완료", indexName);
                }
            }

            elasticsearchOperations.bulkIndex(items, StoreSearchDoc.class);
        } catch (Exception e) {
            log.error("Index 생성 중 에러 발생", e);
            throw e;
        }
    }
}
