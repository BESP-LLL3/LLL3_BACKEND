package com.sangchu.batch.preprocess.job;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.patch.service.StoreHelperService;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StoreReader implements ItemReader<List<Store>>, ItemStream {

    @Autowired
    private StoreHelperService storeHelperService;
    private int currentPage = 0;

    @Override
    public void open(ExecutionContext executionContext) {
        if (executionContext.containsKey("store.currentPage")) {
            currentPage = executionContext.getInt("store.currentPage");
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putInt("store.currentPage", currentPage);
    }

    @Override
    public List<Store> read() {
        List<Store> stores = storeHelperService.getStoreWithoutFranchise(currentPage++, 1000);

        return stores.isEmpty() ? null : stores;
    }
}
