package com.sangchu.global.util;

import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Slf4j
public class UtilFile {
    public static void resetDirectory(Path downloadDir) {
        if (Files.exists(downloadDir)) {
            try {
                Files.walk(downloadDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error("디렉토리 내부 파일 삭제 실패: {}", path, e);
                                throw new CustomException(ApiStatus._FILE_DELETE_FAILED);
                            }
                        });
                log.info("기존 폴더 삭제 완료: {}", downloadDir);
            } catch (IOException e) {
                throw new CustomException(ApiStatus._FILE_DELETE_FAILED);
            }
        }

        try {
            Files.createDirectories(downloadDir);
            log.info("폴더 새로 생성됨: {}", downloadDir);
        } catch (IOException e) {
            throw new CustomException(ApiStatus._FILE_DELETE_FAILED);
        }
    }

    public static String extractCrtrYmFromFileName(String filename) {
        String[] parts = filename.split("_");
        return parts[parts.length - 1].replace(".csv", "");
    }

    public static String getAnyCsvFileName(Path dirPath) {
        File folder = dirPath.toFile();
        File[] csvFiles = folder.listFiles((dir, name) -> name.endsWith(".csv"));

        if (csvFiles != null && csvFiles.length > 0) {
            return csvFiles[0].getName();
        }

        return null;
    }
}
