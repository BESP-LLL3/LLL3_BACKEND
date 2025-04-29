package com.sangchu.preprocess.etl.service;

import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.UtilFile;
import com.sangchu.global.util.statuscode.ApiStatus;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {
    // csv 파일을 가져오는 메서드
    public void crwalingCsvData() {
        crwaling();
        fileUnZip();
    }

    /**
    * 공공데이터포털에서 소상공인진흥공단 상가(상권)정보를 찾아
    * 다운받은 압축 파일을 resources/data에 저장
    */
    private void crwaling() {
        WebDriverManager.chromedriver().setup();

        Path resourcePath = Paths.get("src/main/resources/data").toAbsolutePath();
        log.info("resourcePath = " + resourcePath);

        UtilFile.resetDirectory(resourcePath);

        Map<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("download.default_directory", resourcePath.toString());
        chromePrefs.put("download.prompt_for_download", false);
        chromePrefs.put("safebrowsing.enabled", true);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", chromePrefs);

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://www.data.go.kr/data/15083033/fileData.do#/layer_data_infomation");

            WebElement downloadBtn = driver.findElement(By.xpath("//a[contains(@onclick, \"fn_fileDataDown('15083033'\")]"));
            downloadBtn.click();
            Thread.sleep(3000);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            Alert alert = wait.until(ExpectedConditions.alertIsPresent());
            alert.accept();

            waitForDownloadToComplete(30);

        } catch (Exception e) {
            log.error("크롤링 실패", e);
            throw new CustomException(ApiStatus._FILE_DOWNLOAD_FAIL);
        } finally {
            driver.quit();
        }
    }

    private void waitForDownloadToComplete(int timeoutSeconds) throws InterruptedException, IOException {
        // 1. 경로 설정
        Path resourcePath = Paths.get("src/main/resources").toAbsolutePath();
        Path downloadDir = resourcePath.resolve("data");

        // 2. 다운로드 완료 대기 로직
        int waited = 0;
        while (waited < timeoutSeconds) {
            boolean hasZip = Files.list(downloadDir)
                .anyMatch(file -> file.toString().endsWith(".zip"));

            if (hasZip) {
                log.info("다운로드 완료됨");
                return;
            }

            Thread.sleep(1000);
            waited++;
        }

        throw new CustomException(ApiStatus._FILE_DOWNLOAD_TIMEOUT);
    }

    public void fileUnZip() {
        Path resourcePath = Paths.get("src/main/resources").toAbsolutePath();
        Path downloadDir = resourcePath.resolve("data");

        List<Charset> charsets = Arrays.asList(
            Charset.forName("MS949"),
            Charset.forName("CP949"),
            StandardCharsets.UTF_8,
            StandardCharsets.ISO_8859_1
        );

        try {
            Files.walk(downloadDir)
                .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".zip"))
                .forEach(zipFilePath -> {
                    try {
                        // 압축 파일 내 CSV 파일명 캐릭터셋 검사
                        Charset extractedCharset = null;
                        for (Charset charset : charsets) {
                            try (ZipFile tempZipFile = new ZipFile(zipFilePath.toFile(), charset)) {
                                extractedCharset = charset;
                                break;
                            } catch (IOException e) { /* 실패하면 무시하고 다음 캐릭터셋으로 넘어감 */ }
                        }

                        ZipFile zipFile = new ZipFile(zipFilePath.toFile(), extractedCharset);

                        zipFile.entries().asIterator().forEachRemaining(zipEntry -> {
                            Path outputPath = downloadDir.resolve(zipEntry.getName());

                            try {
                                if (zipEntry.isDirectory()) {
                                    Files.createDirectories(outputPath);
                                } else {
                                    // 부모 디렉토리 없을 경우 생성
                                    if (outputPath.getParent() != null) {
                                        Files.createDirectories(outputPath.getParent());
                                    }

                                    // 파일 복사
                                    try (InputStream zipStream = zipFile.getInputStream(zipEntry)) {
                                        Files.copy(zipStream, outputPath);
                                    }
                                }
                            } catch (IOException e) {
                                log.error("파일 복사 실패: {}", zipEntry.getName(), e);
                            }
                        });

                        log.info("압축 해제 완료: {}", zipFilePath.getFileName());
                    } catch (Exception e) {
                        log.error("압축 해제 실패: {}", zipFilePath, e);
                        throw new CustomException(ApiStatus._FILE_UNZIP_FAILED);
                    }
                });
        } catch (IOException e) {
            log.error("파일 목록 가져오기 실패", e);
            throw new CustomException(ApiStatus._FILE_UNZIP_FAILED);
        }
    }
}