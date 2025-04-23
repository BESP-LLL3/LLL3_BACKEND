package com.sangchu.batch.patch.controller;

import com.sangchu.batch.patch.job.StoreBatchJobLauncher;
import com.sangchu.batch.patch.service.CrawlerService;
import com.sangchu.batch.preprocess.service.PreprocessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Paths;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final PreprocessService preprocessService;
    private final CrawlerService crawlerService;
    private final StoreBatchJobLauncher storeBatchJobLauncher;

    @GetMapping("/Test")
    public void Test() throws IOException {
        crawlerService.getStoreCsvToMySql();
    }

    @PostMapping("/Test-Batch")
    public void TestBatch() throws Exception {
        String resourcePath = String.valueOf(Paths.get("src/main/resources").toAbsolutePath());
        String crtrYm = "202703";
        storeBatchJobLauncher.runJob(resourcePath, crtrYm);
    }
}