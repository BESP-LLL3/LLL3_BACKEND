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

    public List<Store> getStoreWithoutFranchise(int page, int size) {
        return storeRepository.findAllByBranchNmIsNullOrBranchNmEquals("", PageRequest.of(page, size)).getContent();
    }
}

