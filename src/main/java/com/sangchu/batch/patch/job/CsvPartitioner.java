package com.sangchu.batch.patch.job;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Component
public class CsvPartitioner implements Partitioner {

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        File folder = new File("src/main/resources/data");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv"));

        Map<String, ExecutionContext> partitions = new HashMap<>();
        for (int i = 0; i < files.length; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putString("fileName", files[i].getAbsolutePath());
            partitions.put("partition" + i, context);
        }

        return partitions;
    }
}
