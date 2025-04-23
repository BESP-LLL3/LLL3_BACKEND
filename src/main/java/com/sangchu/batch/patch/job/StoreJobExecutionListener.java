package com.sangchu.batch.patch.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;

@Component
public class StoreJobExecutionListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(StoreJobExecutionListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("===== [JOB 시작] Store CSV Batch Job Started. JobInstance: {} =====", jobExecution.getJobInstance());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("===== [JOB 완료] Store CSV Batch Job Completed Successfully =====");
        } else {
            log.error("===== [JOB 실패] Store CSV Batch Job Failed with status: {} =====", jobExecution.getStatus());
        }
    }
}

