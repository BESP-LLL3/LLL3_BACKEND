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

    public List<Store> getStoreByIdRange(Long startId, Long endId, int size) {
        String crtrYm = esHelperService.getRecentCrtrYm();
        return storeRepository.findStoresInRange(startId, endId, size, crtrYm);
    }

    public long getMaxIdByCrtrYm(String crtrYm) {
        return storeRepository.findMaxIdByCrtrYm(crtrYm);
    }

    public long getMinIdByCrtrYm(String crtrYm) {
        return storeRepository.findMinIdByCrtrYm(crtrYm);
    }
}

