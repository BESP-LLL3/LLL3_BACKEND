package com.sangchu.branding.entity;

import lombok.Data;
import java.util.List;

@Data
public class BrandNameRequestDto {
    private String searchWord;         // 검색한 단어 (필수)
    private List<String> trendKeywords; // 트렌드 단어 목록
    private String additionalKeyword; // 추가 입력 키워드 (선택)
    private int limit;             // 추천 개수
}
