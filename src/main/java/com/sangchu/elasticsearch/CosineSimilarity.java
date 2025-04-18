package com.sangchu.elasticsearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.ai.embedding.Embedding;
import org.springframework.stereotype.Component;

import com.sangchu.embedding.entity.StoreSearchDoc;
import com.sangchu.embedding.repository.StoreSearchDocRepository;
import com.sangchu.embedding.service.EmbeddingHelperService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CosineSimilarity {
	private final EmbeddingHelperService embeddingHelperService;
	private final MorphologicalAnalysis morphologicalAnalysis;
	private final StoreSearchDocRepository elasticRepository;

	public static double cosineSimilarity(Embedding vec1, float[] vec2) {
		float[] a = vec1.getOutput();
		float[] b = vec2;

		if (a.length != b.length) {
			//TODO: Exception 추후 수정
			throw new IllegalArgumentException("벡터 길이 불일치");
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
		Embedding keywordEmbedding = embeddingHelperService.getEmbedding(keyword);

		Iterable<StoreSearchDoc> iterable = elasticRepository.findAll();
		List<StoreSearchDoc> allDocs = StreamSupport.stream(iterable.spliterator(), false).toList();

		// 1. 유사도 필터링 (0.7 이상)
		List<StoreSearchDoc> similarDocs = allDocs.stream()
			.filter(doc -> cosineSimilarity(keywordEmbedding, doc.getVector()) >= 0.7)
			.toList();

		// 2. 형태소 분석 및 단어 빈도 집계
		Map<String, Integer> wordFrequency = new HashMap<>();

		for (StoreSearchDoc doc : similarDocs) {
//            // TODO: Document에서 token들을 갖는 필드 추가 후 getTokens()로 변경
			// List<String> tokens = doc.getTokens();
			List<String> tokens = null;
			for (String token : tokens) {
				wordFrequency.put(token, wordFrequency.getOrDefault(token, 0) + 1);
			}
		}

		return wordFrequency;
	}
}
