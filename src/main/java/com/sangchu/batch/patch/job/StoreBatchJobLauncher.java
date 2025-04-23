package com.sangchu.batch.patch.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class StoreBatchJobLauncher {

    private final JobLauncher jobLauncher;
    private final Job storeCsvJob;

    public void runJob(String fileName, String crtrYm) throws Exception {
        // JobParameters를 생성합니다.
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("fileName", fileName)     // 파일명 파라미터
                .addString("crtrYm", "202003")         // 생성 년월 파라미터
                .addLong("timestamp", System.currentTimeMillis()) // 타임스탬프 (중복 방지용)
                .toJobParameters();

        // 배치 작업을 실행합니다.
        jobLauncher.run(storeCsvJob, jobParameters);
    }

    public void runAllCsvJobs() throws Exception {
        String dataDir = "src/main/resources/data";
        File folder = new File(dataDir);

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv"));

        if (files != null) {
            for (File file : files) {
                runJob(file.getAbsolutePath(), extractCrtrYmFromFile(file.getName()));
            }
        }
    }

    private String extractCrtrYmFromFile(String fileName) {
        // 예시: 파일 이름이 store_202003.csv 형태라고 가정
        Pattern pattern = Pattern.compile(".*_(\\d{6})\\.csv");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("파일명에서 crtrYm을 추출할 수 없습니다: " + fileName);
        }
    }
}