package com.sangchu.preprocess.etl.repository;

import com.sangchu.preprocess.etl.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Integer> {

    Page<Store> findAllByBranchNmIsNullOrBranchNmEquals(String empty, Pageable pageable);

    @Query(value = """
    SELECT * FROM store
    WHERE (branch_nm IS NULL OR branch_nm = :empty)
    AND id > :lastId
    AND crtr_ym = :crtrYm
    ORDER BY id ASC
    LIMIT :size
    """, nativeQuery = true)
    List<Store> findRecentStoresAfterId(@Param("empty") String empty, @Param("lastId") Integer lastId, @Param("size") int size, @Param("crtrYm") String crtrYm);
}
