package com.sangchu.elasticsearch.entity;

import org.springframework.ai.embedding.Embedding;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sangchu.preprocess.etl.entity.Store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "store_search_doc-*")
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class StoreSearchDoc {

	@Id
	@Field(name = "id")
	private String id;

	@Field(type = FieldType.Text)
	private String storeId;

	@Field(type = FieldType.Text)
	private String storeNm;

	@Field(type = FieldType.Keyword)
	private String midCatNm;

	@Field(type = FieldType.Keyword)
	private String smallCatNm;

	@Field(type = FieldType.Keyword)
	private List<String> tokens;

	// 768 <- huggingface
	@Field(type = FieldType.Dense_Vector, dims = 1536)
	private float[] vector;

	public void setVectorFromAi(float[] vector) {
		this.vector = vector;
	}

    public StoreSearchDoc(Store store, Embedding embedding, List<String> tokens) {
        this.storeId = store.getStoreId();
		this.storeNm = store.getStoreNm();
        this.midCatNm = store.getMidCatNm();
        this.smallCatNm = store.getSmallCatNm();
        this.setVectorFromAi(embedding.getOutput());
        this.tokens = tokens;
    }

}

