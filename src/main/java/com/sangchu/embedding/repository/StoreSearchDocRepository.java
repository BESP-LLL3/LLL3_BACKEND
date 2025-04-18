package com.sangchu.embedding.repository;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.sangchu.embedding.entity.StoreSearchDoc;

@Repository
public interface StoreSearchDocRepository extends ElasticsearchRepository<StoreSearchDoc, String>{

}
