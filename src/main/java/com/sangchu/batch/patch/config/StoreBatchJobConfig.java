package com.sangchu.batch.patch.config;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.patch.entity.StoreRequestDto;
import com.sangchu.batch.patch.job.*;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class StoreBatchJobConfig {

    private final FlatFileItemReader<StoreRequestDto> storeItemReader;
    private final StoreItemProcessor storeItemProcessor;
    private final StoreItemWriter storeItemWriter;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final StoreJobExecutionListener jobExecutionListener;
    private final StoreStepExecutionListener stepExecutionListener;

    @Bean
    public Job storeCsvJob(Step storeStepToSQL) {
        return new JobBuilder("storeCsvJob", jobRepository)
                .listener(jobExecutionListener)
                .start(storeStepToSQL)
                .build();
    }

    @Bean
    public Step storeStepToSQL() {
        return new StepBuilder("storeStepToSQL", jobRepository)
                .<StoreRequestDto, Store>chunk(1000, transactionManager)
                .reader(storeItemReader)
                .processor(storeItemProcessor)
                .writer(storeItemWriter)
                .listener(stepExecutionListener)
                .faultTolerant()
                .skipLimit(100)
                .skip(Exception.class)
                .taskExecutor(patchTaskExecutor())
                .build();
    }

    @Bean
    public TaskExecutor patchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4); // 스레드 풀 크기
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("store-task-");
        executor.initialize();
        return executor;
    }
}