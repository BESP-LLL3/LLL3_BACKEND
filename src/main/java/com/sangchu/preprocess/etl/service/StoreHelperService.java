package com.sangchu.preprocess.etl.service;

import com.sangchu.elasticsearch.service.EsHelperService;
import com.sangchu.preprocess.etl.entity.Store;
import com.sangchu.preprocess.etl.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreHelperService {
    private final StoreRepository storeRepository;
    private final EsHelperService esHelperService;

    public List<Store> getStoreWithoutFranchise(Long lastId, int size) {
        Long cursor = lastId != null ? lastId : 0;
        String crtrYm = esHelperService.getRecentCrtrYm();

        return storeRepository.findRecentStoresAfterId("", Math.toIntExact(cursor), size, crtrYm);
    }
}

