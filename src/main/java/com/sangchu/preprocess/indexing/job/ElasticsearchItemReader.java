package com.sangchu.preprocess.indexing.job;

import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import com.sangchu.preprocess.etl.entity.Store;
import com.sangchu.preprocess.etl.service.StoreHelperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ElasticsearchItemReader implements ItemReader<List<Store>>, ItemStream {

    @Autowired
    private StoreHelperService storeHelperService;
    private Long lastSeenId = 0L;

    @Override
    public void open(ExecutionContext executionContext) {
        lastSeenId = executionContext.getLong("store.lastSeenId", 0L);
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong("store.lastSeenId", lastSeenId);
    }

    @Override
    public List<Store> read() {
        // ID 기반으로 읽기
        try {
            log.info("ID {} 이후 데이터 읽기 시작", lastSeenId);
            List<Store> stores = storeHelperService.getStoreWithoutFranchise(lastSeenId, 1000);

            if (stores.isEmpty()) {
                return null; // 더 이상 읽을 데이터가 없으면 null 리턴 → Step 종료
            }

            // 마지막으로 본 id 업데이트
            lastSeenId = stores.get(stores.size() - 1).getId();

            return stores;
        } catch (Exception e) {
            throw new CustomException(ApiStatus._READ_FAIL, "ID " + lastSeenId + " 이후 처리 중 에러 발생");
        }
    }
}
