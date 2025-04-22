package com.sangchu.batch.preprocess.config;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.preprocess.job.StoreProcessor;
import com.sangchu.batch.preprocess.job.StoreReader;
import com.sangchu.batch.preprocess.job.StoreWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class StoreJobConfig {

    private final StoreReader storeReader;
    private final StoreProcessor storeProcessor;
    private final StoreWriter storeWriter;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job storeEmbeddingJob() {
        return new JobBuilder("storePreprocessJob", jobRepository)
                .start(storeStep())
                .build();
    }

    @Bean
    @Transactional(propagation = Propagation.REQUIRED)
    public Step storeStep() {
        return new StepBuilder("storeStep", jobRepository)
                .<List<Store>, List<IndexQuery>>chunk(1, transactionManager)
                .reader(storeReader)
                .processor(storeProcessor)
                .writer(storeWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4); // 스레드 풀 크기
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("store-task-");
        executor.initialize();
        return executor;
    }
}
