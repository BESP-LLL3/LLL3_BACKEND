package com.sangchu.batch.preprocess.config;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.preprocess.job.ElasticsearchItemProcessor;
import com.sangchu.batch.preprocess.job.ElasticsearchItemReader;
import com.sangchu.batch.preprocess.job.ElasticsearchItemWriter;
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
public class ElasticsearchBatchConfig {

    private final ElasticsearchItemReader elasticsearchItemReader;
    private final ElasticsearchItemProcessor elasticsearchItemProcessor;
    private final ElasticsearchItemWriter elasticsearchItemWriter;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job elasticsearchJob() {
        return new JobBuilder("elasticsearchJob", jobRepository)
                .start(elasticsearchStep())
                .build();
    }

    @Bean
    @Transactional(propagation = Propagation.REQUIRED)
    public Step elasticsearchStep() {
        return new StepBuilder("elasticsearchStep", jobRepository)
                .<List<Store>, List<IndexQuery>>chunk(1, transactionManager)
                .reader(elasticsearchItemReader)
                .processor(elasticsearchItemProcessor)
                .writer(elasticsearchItemWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100)
                .taskExecutor(elasticsearchTaskExecutor())
                .build();
    }

    @Bean
    public TaskExecutor elasticsearchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("elasticsearch-import-task-");
        executor.initialize();
        return executor;
    }
}
