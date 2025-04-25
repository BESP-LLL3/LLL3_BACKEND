package com.sangchu.preprocess.indexing.job;

import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import com.sangchu.preprocess.etl.entity.Store;
import com.sangchu.elasticsearch.MorphologicalAnalysis;
import com.sangchu.elasticsearch.entity.StoreSearchDoc;
import com.sangchu.embedding.service.EmbeddingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.embedding.Embedding;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLEngineResult;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchItemProcessor implements ItemProcessor<List<Store>, List<IndexQuery>> {

	private final EmbeddingService embeddingService;
	private final MorphologicalAnalysis morphologicalAnalysis;

	@Override
	public List<IndexQuery> process(List<Store> stores) {
		List<String> contexts = stores.stream().map(this::makeContextString).toList();

		List<Embedding> embeddings = embeddingService.getBatchEmbeddings(contexts);

		try {
			List<IndexQuery> result = IntStream.range(0, stores.size()).mapToObj(i -> {
				Store store = stores.get(i);
				String crtrYm = store.getCrtrYm();

				List<String> tokens = morphologicalAnalysis.extractNouns(store.getStoreNm());
				StoreSearchDoc doc = new StoreSearchDoc(store, embeddings.get(i), tokens);

				return new IndexQueryBuilder().withId(doc.getStoreId())
					.withIndex("store_search_doc-" + crtrYm)
					.withObject(doc)
					.build();
			}).toList();

			log.info("IndexQuery 생성 성공! {}", result.size());
			return result;

		} catch (Exception e) {
			throw new CustomException(ApiStatus._ES_INDEX_QUERY_CREATE_FAIL);
		}
	}

	private String makeContextString(Store store) {
		// 강조 문구 + 문장 결합
		return "상가 정보입니다. 상호명(중요!): " + store.getStoreNm() + ", 업종중분류: " + store.getMidCatNm() + ", 업종소분류: "
			+ store.getSmallCatNm();
	}
}
