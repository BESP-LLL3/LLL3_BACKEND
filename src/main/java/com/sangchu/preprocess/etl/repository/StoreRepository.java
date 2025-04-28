package com.sangchu.preprocess.etl.repository;

import com.sangchu.preprocess.etl.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Integer> {

    @Query(value = """
    SELECT * FROM store
    WHERE (branch_nm IS NULL OR branch_nm = "")
    AND id > :startId AND id <= :endId
    AND crtr_ym = :crtrYm
    ORDER BY id ASC
    LIMIT :size
    """, nativeQuery = true)
    List<Store> findStoresInRange(@Param("startId") Long startId,
                                  @Param("endId") Long endId,
                                  @Param("size") int size,
                                  @Param("crtrYm") String crtrYm);

    @Query("SELECT MAX(s.id) FROM Store s WHERE s.crtrYm = :crtrYm")
    long findMaxIdByCrtrYm(@Param("crtrYm") String crtrYm);

    @Query("SELECT MIN(s.id) FROM Store s WHERE s.crtrYm = :crtrYm")
    long findMinIdByCrtrYm(@Param("crtrYm") String crtrYm);
}
