package com.sangchu.prefer.mapper;

import com.sangchu.prefer.entity.PreferName;
import com.sangchu.prefer.entity.PreferNameCreateDto;

public class PreferMapper {

    public static PreferName toEntity(PreferNameCreateDto preferNameCreateDto) {
        return PreferName.builder()
                .keyword(preferNameCreateDto.getKeyword())
                .custom(preferNameCreateDto.getCustom())
                .name(preferNameCreateDto.getName())
                .build();
    }
}
