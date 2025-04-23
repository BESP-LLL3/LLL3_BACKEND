package com.sangchu.batch.patch.config;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.patch.entity.StoreRequestDto;
import com.sangchu.batch.patch.job.CsvPartitioner;
import com.sangchu.batch.patch.job.MysqlItemProcessor;
import com.sangchu.batch.patch.job.MysqlItemReader;
import com.sangchu.batch.patch.job.MysqlItemWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class MysqlBatchConfig {

    private final MysqlItemProcessor storeItemProcessor;
    private final MysqlItemWriter storeItemWriter;
    private final PlatformTransactionManager transactionManager;
    private final JobRepository jobRepository;
    private final CsvPartitioner csvPartitioner;

    @Bean
    @StepScope
    public MultiResourceItemReader<StoreRequestDto> storeCsvReader() {
        return new MysqlItemReader().multiResourceItemReader();
    }

    @Bean
    public Job mysqlJob() {
        return new JobBuilder("mysqlJob", jobRepository)
                .start(mysqlPartitionStep(jobRepository))
                .build();
    }

    @Bean
    public Step mysqlStep() {
        return new StepBuilder("mysqlStep", jobRepository)
                .<StoreRequestDto, Store>chunk(1000, transactionManager)
                .reader(storeCsvReader())
                .processor(storeItemProcessor)
                .writer(storeItemWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100)
                .taskExecutor(mysqlTaskExecutor()) // 병렬 처리
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<StoreRequestDto> csvReader(@Value("#{stepExecutionContext['fileName']}") String fileName) {
        FlatFileItemReader<StoreRequestDto> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(fileName));
        reader.setLinesToSkip(1);
        reader.setLineMapper(new DefaultLineMapper<>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames("storeId", "storeNm", "branchNm", "largeCatCd", "largeCatNm", "midCatCd", "midCatNm", "smallCatCd", "smallCatNm", "ksicCd", "ksicNm", "sidoCd", "sidoNm", "sggCd", "sggNm", "hDongCd", "hDongNm", "bDongCd", "bDongNm", "lotNoCd", "landDivCd", "landDivNm", "lotMainNo", "lotSubNo", "lotAddr", "roadCd", "roadNm", "bldgMainNo", "bldgSubNo", "bldgMgmtNo", "bldgNm", "roadAddr", "oldZipCd", "newZipCd", "block", "floor", "room", "coordX", "coordY");
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                setTargetType(StoreRequestDto.class);
            }});
        }});
        return reader;
    }

    @Bean
    public Step mysqlSlaveStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("mysqlSlaveStep", jobRepository)
                .<StoreRequestDto, Store>chunk(1000, transactionManager)
                .reader(csvReader(null))
                .processor(storeItemProcessor)
                .writer(storeItemWriter)
                .build();
    }

    @Bean
    public Step mysqlPartitionStep(JobRepository jobRepository) {
        return new StepBuilder("mysqlPartitionStep", jobRepository)
                .partitioner("mysqlSlaveStep", csvPartitioner)
                .step(mysqlSlaveStep(jobRepository, transactionManager))
                .gridSize(4)
                .taskExecutor(mysqlTaskExecutor())
                .build();
    }

    @Bean
    public TaskExecutor mysqlTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("mysql-task-");
        executor.initialize();
        return executor;
    }
}
