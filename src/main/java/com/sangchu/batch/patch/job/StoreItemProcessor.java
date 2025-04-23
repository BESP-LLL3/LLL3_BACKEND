package com.sangchu.batch.patch.job;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.patch.entity.StoreRequestDto;
import com.sangchu.batch.patch.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class StoreItemProcessor implements ItemProcessor<StoreRequestDto, Store> {

    @Value("#{jobParameters['crtrYm']}")
    private String crtrYm;

    @Override
    public Store process(StoreRequestDto item) throws Exception {
        return StoreMapper.toEntity(crtrYm, item);
    }
}
