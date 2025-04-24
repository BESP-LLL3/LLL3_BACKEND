package com.sangchu.preprocess.etl.service;

import com.sangchu.preprocess.etl.entity.Store;
import com.sangchu.preprocess.etl.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreHelperService {
    private final StoreRepository storeRepository;

    // ID 기반으로 프랜차이즈가 없는 매장 목록을 가져오는 메서드
    public List<Store> getStoreWithoutFranchise(Long lastId, int size) {
        // lastId가 null이면 0으로 초기화
        Long cursor = lastId != null ? lastId : 0;
        return storeRepository.findRecentStoresAfterId("", Math.toIntExact(cursor), size);
    }
}

