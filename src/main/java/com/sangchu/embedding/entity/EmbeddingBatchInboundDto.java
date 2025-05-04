package com.sangchu.embedding.entity;

import lombok.Data;

import java.util.List;

@Data
public class EmbeddingBatchInboundDto {
    private List<float[]> embeddings;
}
