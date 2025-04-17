package com.sangchu.batch.preprocess.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "store_search_doc")
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


	// 768 <- huggingface
	@Field(type = FieldType.Dense_Vector, dims = 1536)
	private float[] vector;

	public void setVectorFromAi(float[] vector) {
		this.vector = vector;
	}

}

