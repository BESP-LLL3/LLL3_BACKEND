package com.sangchu.batch.patch.job;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.patch.repository.StoreRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StoreItemWriter implements ItemWriter<Store> {
    private final StoreRepository storeRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void write(Chunk<? extends Store> items) throws Exception {
        int batchSize = 1000;  // flush()를 주기적으로 호출할 배치 사이즈 설정

        for (int i = 0; i < items.size(); i++) {
            storeRepository.save(items.getItems().get(i));

            // 배치 사이즈마다 flush()와 clear() 호출
            if (i % batchSize == 0 && i > 0) {
                entityManager.flush();  // DB에 반영
                entityManager.clear();  // 영속성 컨텍스트 비움
            }
        }

        // 마지막 배치 후에도 flush() 호출
        entityManager.flush();
        entityManager.clear();
    }
}