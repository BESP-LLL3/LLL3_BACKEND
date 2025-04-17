package com.sangchu.batch.patch.repository;

import com.sangchu.batch.patch.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Integer> {

    List<Store> findAllByBranchNmIsNullOrBranchNmEquals(String empty);
}
