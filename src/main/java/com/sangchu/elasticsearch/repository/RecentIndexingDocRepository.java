package com.sangchu.elasticsearch.repository;

import com.sangchu.elasticsearch.entity.RecentIndexingDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface RecentIndexingDocRepository extends ElasticsearchRepository<RecentIndexingDoc, String> {

}
