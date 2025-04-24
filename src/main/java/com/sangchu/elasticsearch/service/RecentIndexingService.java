package com.sangchu.elasticsearch.service;

import com.sangchu.elasticsearch.entity.RecentIndexingDoc;
import com.sangchu.elasticsearch.repository.RecentIndexingDocRepository;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecentIndexingService {
    private final RecentIndexingDocRepository recentIndexingDocRepository;

    public void indexRecentCrtrYm(String crtrYm) throws Exception {
        try {
            RecentIndexingDoc doc = RecentIndexingDoc.builder()
                    .id("recent_crtr_ym")
                    .crtrYm(crtrYm)
                    .build();

            recentIndexingDocRepository.save(doc);
        } catch (RuntimeException e) {
            throw new CustomException(ApiStatus._INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("최근 분기 인덱싱 중 예외 발생 - crtrYm : {}", crtrYm, e);
            throw e;
        }
    }
}
