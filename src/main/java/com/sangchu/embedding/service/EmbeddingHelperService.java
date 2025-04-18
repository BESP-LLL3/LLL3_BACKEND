package com.sangchu.embedding.service;

import org.springframework.ai.embedding.Embedding;
import org.springframework.stereotype.Service;

import com.sangchu.embedding.util.EmbeddingUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmbeddingHelperService {

	public Embedding getEmbedding(String keyword) {
		return EmbeddingUtil.getEmbedding(keyword);
	}
}
