package com.sangchu.batch.patch.job;

import com.sangchu.batch.patch.entity.StoreRequestDto;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.*;

@StepScope
public class MysqlItemReader {

	public MultiResourceItemReader<StoreRequestDto> multiResourceItemReader() {
		File folder = new File("src/main/resources/data");
		if (!folder.exists() || !folder.isDirectory()) {
			throw new IllegalArgumentException("CSV 파일 경로가 잘못되었습니다: " + folder.getAbsolutePath());
		}

		Resource[] resources = Arrays.stream(
				Objects.requireNonNull(folder.listFiles((dir, name) -> name.endsWith(".csv"))))
			.map(FileSystemResource::new)
			.toArray(Resource[]::new);

		return new MultiResourceItemReaderBuilder<StoreRequestDto>().name("multiCsvReader")
			.resources(resources)
			.delegate(singleCsvReader())
			.build();
	}

	private FlatFileItemReader<StoreRequestDto> singleCsvReader() {
		FlatFileItemReader<StoreRequestDto> reader = new FlatFileItemReader<>();
		reader.setLinesToSkip(1); // 헤더 스킵
		reader.setLineMapper(new DefaultLineMapper<>() {{
			setLineTokenizer(new DelimitedLineTokenizer() {{
				setNames("storeId", "storeNm", "branchNm", "largeCatCd", "largeCatNm", "midCatCd", "midCatNm",
					"smallCatCd", "smallCatNm", "ksicCd", "ksicNm", "sidoCd", "sidoNm", "sggCd", "sggNm", "hDongCd",
					"hDongNm", "bDongCd", "bDongNm", "lotNoCd", "landDivCd", "landDivNm", "lotMainNo", "lotSubNo",
					"lotAddr", "roadCd", "roadNm", "bldgMainNo", "bldgSubNo", "bldgMgmtNo", "bldgNm", "roadAddr",
					"oldZipCd", "newZipCd", "block", "floor", "room", "coordX", "coordY");
			}});
			setFieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
				setTargetType(StoreRequestDto.class);
			}});
		}});
		return reader;
	}
}
