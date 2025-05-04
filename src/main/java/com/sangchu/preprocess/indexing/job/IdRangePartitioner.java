package com.sangchu.preprocess.indexing.job;

import com.sangchu.elasticsearch.service.EsHelperService;
import com.sangchu.preprocess.etl.service.StoreHelperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdRangePartitioner implements Partitioner {

    private final StoreHelperService storeHelperService;
    private final EsHelperService esHelperService;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        String crtrYm = esHelperService.getRecentCrtrYm();
        long minId = storeHelperService.getMinIdByCrtrYm(crtrYm);
        long maxId = storeHelperService.getMaxIdByCrtrYm(crtrYm);

        long targetSize = (maxId - minId) / gridSize + 1;

        Map<String, ExecutionContext> result = new HashMap<>();

        long start = minId;
        long end = start + targetSize - 1;

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putLong("startId", start);
            context.putLong("endId", Math.min(end, maxId));
            result.put("partition" + i, context);

            start = end + 1;
            end = start + targetSize - 1;
        }

        return result;
    }
}
