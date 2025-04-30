package com.sangchu.prefer.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PreferNameCreateDto {

    private String keyword;
    private String custom;
    private String name;
}
