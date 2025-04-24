package com.sangchu.preprocess.etl.job;

import com.sangchu.preprocess.etl.entity.Store;
import com.sangchu.preprocess.etl.entity.StoreRequestDto;
import com.sangchu.preprocess.etl.mapper.StoreMapper;

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
