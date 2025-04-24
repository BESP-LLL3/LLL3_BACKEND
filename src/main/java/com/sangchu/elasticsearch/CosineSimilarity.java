package com.sangchu.elasticsearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sangchu.elasticsearch.service.EsHelperService;
import com.sangchu.embedding.service.EmbeddingService;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import org.springframework.ai.embedding.Embedding;
import org.springframework.stereotype.Component;

import com.sangchu.elasticsearch.entity.StoreSearchDoc;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CosineSimilarity {
	private final EmbeddingService embeddingService;
	private final EsHelperService esHelperService;

	public static double cosineSimilarity(Embedding vec1, float[] vec2) {
		float[] a = vec1.getOutput();
		float[] b = vec2;

		if (a.length != b.length) {
			throw new CustomException(ApiStatus._VECTOR_LENGTH_DIFFERENT);
		}

		double dot = 0.0, normA = 0.0, normB = 0.0;
		for (int i = 0; i < a.length; i++) {
			dot += a[i] * b[i];
			normA += Math.pow(a[i], 2);
			normB += Math.pow(b[i], 2);
		}

		if (normA == 0 || normB == 0) {
			return 0.0; // 0벡터일 경우 유사도 정의 불가
		}

		return dot / (Math.sqrt(normA) * Math.sqrt(normB));
	}

	public Map<String, Integer> getWordFrequency(String keyword) {
		Embedding keywordEmbedding = embeddingService.getEmbedding(keyword);

		List<StoreSearchDoc> allDocs = esHelperService.findRecentCrtrYmDocs();
		// 1. 유사도 필터링 (0.5 이상)
		List<StoreSearchDoc> similarDocs = allDocs.stream()
			.filter(doc -> cosineSimilarity(keywordEmbedding, doc.getVector()) >= 0.5)
			.toList();

		// 2. 형태소 분석 및 단어 빈도 집계
		Map<String, Integer> wordFrequency = new HashMap<>();

		for (StoreSearchDoc doc : similarDocs) {
			 List<String> tokens = doc.getTokens();
			for (String token : tokens) {
				wordFrequency.put(token, wordFrequency.getOrDefault(token, 0) + 1);
			}
		}

		return wordFrequency;
	}
}
