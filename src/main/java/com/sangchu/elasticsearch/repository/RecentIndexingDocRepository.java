package com.sangchu.elasticsearch.repository;

import com.sangchu.elasticsearch.entity.RecentIndexingDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface RecentIndexingDocRepository extends ElasticsearchRepository<RecentIndexingDoc, String> {
    Optional<RecentIndexingDoc> findById(String id);
}
