package com.sangchu.preprocess.indexing.job;

import com.sangchu.preprocess.etl.entity.Store;
import com.sangchu.preprocess.etl.service.StoreHelperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;

import java.util.List;

@StepScope
@Slf4j
public class ElasticsearchItemReader implements ItemReader<List<Store>>, ItemStream {

    private final StoreHelperService storeHelperService;

    private long startId;
    private long endId;
    private long lastSeenId;

    public ElasticsearchItemReader(StoreHelperService storeHelperService, long startId, long endId) {
        this.storeHelperService = storeHelperService;
        this.startId = startId;
        this.endId = endId;
        this.lastSeenId = startId - 1;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        if (executionContext.containsKey("lastSeenId")) {
            lastSeenId = executionContext.getLong("lastSeenId");
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong("lastSeenId", lastSeenId);
    }

    @Override
    public List<Store> read() {
        if (lastSeenId >= endId) return null;

        log.info("MYSQL ID {}부터 읽는중...", lastSeenId);
        List<Store> stores = storeHelperService.getStoreByIdRange(lastSeenId, endId, 1000);

        if (stores.isEmpty()) return null;

        lastSeenId = stores.get(stores.size() - 1).getId() + 1;

        return stores;
    }
}
