package com.sangchu.batch.patch.service;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.patch.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreHelperService {
    private final StoreRepository storeRepository;

    // 페이지네이션을 사용하여 프랜차이즈가 없는 매장 목록을 가져오는 메서드
//    public List<Store> getStoreWithoutFranchise(int page, int size) {
//        return storeRepository.findAllByBranchNmIsNullOrBranchNmEquals("", PageRequest.of(page, size)).getContent();
//    }

    // ID 기반으로 프랜차이즈가 없는 매장 목록을 가져오는 메서드
    public List<Store> getStoreWithoutFranchise(Long lastId, int size) {
        // lastId가 null이면 0으로 초기화
        Long cursor = lastId != null ? lastId : 0;
        return storeRepository.findStoresAfterId("", Math.toIntExact(cursor), size);
    }
}

