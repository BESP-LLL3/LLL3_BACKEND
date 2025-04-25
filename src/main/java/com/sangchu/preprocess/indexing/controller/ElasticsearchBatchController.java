package com.sangchu.preprocess.indexing.controller;

import com.sangchu.elasticsearch.service.EsHelperService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class ElasticsearchBatchController {

    private final JobLauncher jobLauncher;
    private final Job elasticsearchJob;
    private final EsHelperService esHelperService;

    @PostMapping("/import/elasticsearch")
    public String runImportElasticsearchJob(@RequestParam String crtrYm) throws Exception {

        String recentCrtrYm = esHelperService.getRecentCrtrYm();
        esHelperService.indexRecentCrtrYm(crtrYm);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(elasticsearchJob, jobParameters);

        esHelperService.indexRecentCrtrYm(recentCrtrYm);

        return "mysql -> elasticsearch 작업";
    }
} 