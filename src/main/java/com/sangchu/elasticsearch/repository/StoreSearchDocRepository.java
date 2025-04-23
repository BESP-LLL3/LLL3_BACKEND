package com.sangchu.elasticsearch.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.sangchu.elasticsearch.entity.StoreSearchDoc;

@Repository
public interface StoreSearchDocRepository extends ElasticsearchRepository<StoreSearchDoc, String>{

}
