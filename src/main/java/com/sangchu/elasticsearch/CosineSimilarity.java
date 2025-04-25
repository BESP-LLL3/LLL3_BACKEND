package com.sangchu.elasticsearch;

import java.util.ArrayList;
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

	public Map<String, Double> getWordRelevance(String keyword, String indexName) {
		Embedding keywordEmbedding = embeddingService.getEmbedding(keyword);

		List<StoreSearchDoc> allDocs = esHelperService.findDocsByName(indexName);
		// 1. 유사도 필터링 (0.2 이상)
		List<Double> similarites = new ArrayList<>();
		List<StoreSearchDoc> similarDocs = allDocs.stream()
				.filter(doc -> {
					double temp = cosineSimilarity(keywordEmbedding, doc.getVector());
					if (temp >= 0.2) {
						similarites.add(temp);
						return true;
					}
					return false;
				})
				.toList();

		// 2. 형태소 분석 및 단어 빈도 집계
		Map<String, Double> wordRelevance = new HashMap<>();
		for (int i = 0; i < similarDocs.size(); i++) {
			StoreSearchDoc doc = similarDocs.get(i);
			double similarity = similarites.get(i);

			List<String> tokens = doc.getTokens();
			for (String token : tokens) {
				wordRelevance.put(token, wordRelevance.getOrDefault(token, 0d) + similarity);
			}
		}
		return wordRelevance;
	}
}