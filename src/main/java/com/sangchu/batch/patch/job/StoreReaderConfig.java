package com.sangchu.batch.patch.job;

import com.sangchu.batch.patch.entity.StoreRequestDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Configuration
public class StoreReaderConfig {

    @StepScope
    @Bean
    public FlatFileItemReader<StoreRequestDto> storeItemReader(@Value("#{jobParameters['filePath']}") String filePath) {
        FlatFileItemReader<StoreRequestDto> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(filePath));
        reader.setLinesToSkip(1);
        reader.setLineMapper(lineMapper());
        return reader;
    }

    private LineMapper<StoreRequestDto> lineMapper() {
        DefaultLineMapper<StoreRequestDto> lineMapper = new DefaultLineMapper<>();

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setNames(
                "storeId", "storeNm", "branchNm", "largeCatCd", "largeCatNm", "midCatCd", "midCatNm", "smallCatCd", "smallCatNm",
                "ksicCd", "ksicNm", "sidoCd", "sidoNm", "sggCd", "sggNm", "hDongCd", "hDongNm", "bDongCd", "bDongNm", "lotNoCd",
                "landDivCd", "landDivNm", "lotMainNo", "lotSubNo", "lotAddr", "roadCd", "roadNm", "bldgMainNo", "bldgSubNo",
                "bldgMgmtNo", "bldgNm", "roadAddr", "oldZipCd", "newZipCd", "block", "floor", "room", "coordX", "coordY"
        );

        BeanWrapperFieldSetMapper<StoreRequestDto> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(StoreRequestDto.class);

        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return lineMapper;
    }
}
