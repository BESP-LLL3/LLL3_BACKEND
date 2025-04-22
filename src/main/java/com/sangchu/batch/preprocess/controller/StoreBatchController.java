package com.sangchu.batch.preprocess.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class StoreBatchController {

    private final JobLauncher jobLauncher;
    private final Job storeEmbeddingJob;

    @PostMapping("/store-embedding")
    public String runStoreEmbeddingJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(storeEmbeddingJob, jobParameters);
        return "Store embedding job started";
    }
} 