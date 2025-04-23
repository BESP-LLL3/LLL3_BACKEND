package com.sangchu.batch.patch.job;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.patch.entity.StoreRequestDto;
import com.sangchu.batch.patch.mapper.StoreMapper;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

@StepScope
public class MysqlItemProcessor implements ItemProcessor<StoreRequestDto, Store> {

    @Value("#{stepExecutionContext['crtrYm']}")
    private String crtrYm;

    @Override
    public Store process(StoreRequestDto item) {

        return StoreMapper.toEntity(crtrYm, item);
    }
}
