package com.sangchu.batch.patch.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;


@Component
public class StoreStepExecutionListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(StoreStepExecutionListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("---- [STEP 시작] Step '{}' is starting ----", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("---- [STEP 종료] Step '{}' finished with status: {} ----", stepExecution.getStepName(), stepExecution.getStatus());
        return stepExecution.getExitStatus();
    }
}
