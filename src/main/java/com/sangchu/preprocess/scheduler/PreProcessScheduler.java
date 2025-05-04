package com.sangchu.preprocess.scheduler;

import com.sangchu.elasticsearch.service.EsHelperService;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.UtilFile;
import com.sangchu.global.util.statuscode.ApiStatus;
import com.sangchu.preprocess.etl.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class PreProcessScheduler {
    private final JobLauncher jobLauncher;
    private final Job mysqlJob;
    private final Job elasticsearchJob;
    private final CrawlerService crawlerService;
    private final EsHelperService esHelperService;

    // 2,5,8,11월 5일 0시
    @Scheduled(cron = "0 0 0 5 2,5,8,11 *")
    public void scheduler() throws Exception {

        // 크롤링 후 압축 해제
        crawlerService.crwalingCsvData();

        Path resourcePath = Path.of("src/main/resources/data");
        String filename = UtilFile.getAnyCsvFileName(resourcePath)
             .orElseThrow(() -> new CustomException(ApiStatus._CSV_READ_FAILED));
        String crtrYm = UtilFile.extractCrtrYmFromFileName(filename)
            .orElseThrow(() -> new CustomException(ApiStatus._FILE_READ_FAILED));

        // crtrYm 엘라스틱 서치에 저장
        esHelperService.indexRecentCrtrYm(crtrYm);

        // 파일 -> mysql, mysql -> 엘라스틱서치 배치 작업 진행
        runBatchJob();

        // csv 파일 삭제
        UtilFile.resetDirectory(resourcePath);
    }

    public void runBatchJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(mysqlJob, jobParameters);
        jobLauncher.run(elasticsearchJob, jobParameters);
    }

}
