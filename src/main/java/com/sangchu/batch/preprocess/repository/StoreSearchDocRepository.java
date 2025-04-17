package com.sangchu.batch.preprocess.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.sangchu.batch.preprocess.entity.StoreSearchDoc;

@Repository
public interface StoreSearchDocRepository extends ElasticsearchRepository<StoreSearchDoc, String>{

}
