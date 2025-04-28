package com.sangchu.preprocess.indexing.config;

import com.sangchu.preprocess.etl.entity.Store;
import com.sangchu.preprocess.etl.service.StoreHelperService;
import com.sangchu.preprocess.indexing.job.*;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ElasticsearchBatchConfig {

    private final ElasticsearchItemProcessor elasticsearchItemProcessor;
    private final ElasticsearchItemWriter elasticsearchItemWriter;
    private final StoreHelperService storeHelperService;
    private final IdRangePartitioner partitioner;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job elasticsearchJob() {
        return new JobBuilder("elasticsearchJob", jobRepository)
                .start(masterStep())
                .build();
    }

    @Bean
    public Step masterStep() {
        return new StepBuilder("masterStep", jobRepository)
                .partitioner("workerStep", partitioner)
                .step(workerStep())
                .gridSize(4)
                .taskExecutor(elasticsearchTaskExecutor())
                .build();
    }

    @Bean
    public Step workerStep() {
        return new StepBuilder("workerStep", jobRepository)
                .<List<Store>, List<IndexQuery>>chunk(1, transactionManager)
                .reader(elasticsearchItemReader(null, null))
                .processor(elasticsearchItemProcessor)
                .writer(elasticsearchItemWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100)
                .build();
    }

    @Bean
    @StepScope
    public ElasticsearchItemReader elasticsearchItemReader(
            @Value("#{stepExecutionContext['startId']}") Long startId,
            @Value("#{stepExecutionContext['endId']}") Long endId) {

        return new ElasticsearchItemReader(storeHelperService, startId, endId);
    }

    @Bean
    public TaskExecutor elasticsearchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("es-task-");
        executor.initialize();
        return executor;
    }
}
