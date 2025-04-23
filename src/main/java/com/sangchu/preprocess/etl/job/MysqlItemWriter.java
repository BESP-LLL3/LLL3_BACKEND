package com.sangchu.preprocess.etl.job;

import com.sangchu.preprocess.etl.entity.Store;
import com.sangchu.preprocess.etl.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MysqlItemWriter implements ItemWriter<Store> {

    private final StoreRepository storeRepository;

    @Override
    public void write(Chunk<? extends Store> chunk) throws Exception {

        storeRepository.saveAll(chunk);
    }
}