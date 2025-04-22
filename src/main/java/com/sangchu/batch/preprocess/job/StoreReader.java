package com.sangchu.batch.preprocess.job;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.patch.service.StoreHelperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class StoreReader implements ItemReader<List<Store>>, ItemStream {

    @Autowired
    private StoreHelperService storeHelperService;
//    private int currentPage = 0;
    private Long lastSeenId = 0L;

    @Override
    public void open(ExecutionContext executionContext) {
//        currentPage = executionContext.getInt("store.currentPage", 0);
        lastSeenId = executionContext.getLong("store.lastSeenId", 0L);
    }

    @Override
    public void update(ExecutionContext executionContext) {
//        executionContext.putInt("store.currentPage", currentPage);
        executionContext.putLong("store.lastSeenId", lastSeenId);
    }

    @Override
    public List<Store> read() {
        // 페이지 기반으로 읽기
//        try {
//            List<Store> stores = storeHelperService.getStoreWithoutFranchise(currentPage++, 1000);
//            return stores.isEmpty() ? null : stores;
//        } catch (Exception e) {
//            log.error("페이지 {} 처리 중 에러 발생", currentPage - 1, e);
//            throw e; // 에러 발생 시 Spring Batch가 ExecutionContext에 상태를 저장
//        }

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
            log.error("ID {} 이후 처리 중 에러 발생", lastSeenId, e);
            throw e;
        }
    }
}
