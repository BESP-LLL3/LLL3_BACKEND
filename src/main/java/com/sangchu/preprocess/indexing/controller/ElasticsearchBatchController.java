package com.sangchu.preprocess.indexing.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class ElasticsearchBatchController {

    private final JobLauncher jobLauncher;
    private final Job elasticsearchJob;

    @PostMapping("/import/elasticsearch")
    public String runImportElasticsearchJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(elasticsearchJob, jobParameters);

        return "mysql -> elasticsearch 작업";
    }
} 