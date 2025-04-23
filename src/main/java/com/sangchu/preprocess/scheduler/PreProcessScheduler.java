package com.sangchu.preprocess.scheduler;

import com.sangchu.global.util.UtilFile;
import com.sangchu.preprocess.etl.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class PreProcessScheduler {
    private final JobLauncher jobLauncher;
    private final Job mysqlJob;
    private final Job elasticsearchJob;
    private final CrawlerService crawlerService;

    // 2,5,8,11월 5일 0시
    @Scheduled(cron = "0 0 0 5 2,5,8,11 *")
    public void scheduler() throws Exception {
        File folder = new File("src/main/resources/data");
        Path resourcePath = folder.toPath();
        String filename = UtilFile.getAnyCsvFileName(resourcePath);

        // 크롤링 후 압축 해제
        crawlerService.crwalingCsvData();
        runBatchJob();
        UtilFile.resetDirectory(resourcePath);
        String crtrYm = UtilFile.extractCrtrYmFromFileName(filename);

        // crtrYm 엘라스틱 서치에 저장
    }

    public void runBatchJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(mysqlJob, jobParameters);
        jobLauncher.run(elasticsearchJob, jobParameters);
    }

}
