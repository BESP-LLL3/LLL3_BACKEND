package com.sangchu.elasticsearch.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "recent_indexing_doc")
@ToString
public class RecentIndexingDoc {
    @Id
    @Field(name = "id")
    private String id;

    @Field(type = FieldType.Text)
    private String crtrYm;

    public RecentIndexingDoc(String crtrYm) {
        this.crtrYm = crtrYm;
    }
}
