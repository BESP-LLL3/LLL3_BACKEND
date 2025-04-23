package com.sangchu.batch.patch.job;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.patch.entity.StoreRequestDto;
import com.sangchu.batch.patch.mapper.StoreMapper;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class MysqlItemProcessor implements ItemProcessor<StoreRequestDto, Store> {

    @Override
    public Store process(StoreRequestDto item) {
        // TODO: 파일명의 기준년월로 설정하기
        return StoreMapper.toEntity("202301", item);
    }
}
