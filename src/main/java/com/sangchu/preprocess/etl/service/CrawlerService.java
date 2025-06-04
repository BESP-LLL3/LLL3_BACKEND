package com.sangchu.preprocess.etl.service;

import com.microsoft.playwright.*;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.UtilFile;
import com.sangchu.global.util.statuscode.ApiStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
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
        Path resourcePath = Paths.get("src/main/resources/data").toAbsolutePath();
        log.info("resourcePath = {}", resourcePath);

        UtilFile.resetDirectory(resourcePath);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setAcceptDownloads(true));

            Page page = context.newPage();
            page.navigate("https://www.data.go.kr/data/15083033/fileData.do#/layer_data_infomation");
            page.onDialog(Dialog::accept);

            Download download = page.waitForDownload(() -> {
                try {
                    page.click("xpath=//a[contains(@onclick, \"fn_fileDataDown('15083033'\")]");
                } catch (PlaywrightException e) {
                    throw new RuntimeException(e);
                }
            });

            Path tmpDownloadedFile = download.path();

            Path targetFile = resourcePath.resolve(download.suggestedFilename());
            Files.copy(tmpDownloadedFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            browser.close();
        } catch (Exception e) {
            log.error("크롤링 실패", e);
            throw new CustomException(ApiStatus._FILE_DOWNLOAD_FAIL);
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
            Charset.forName("EUC-KR"),
            Charset.forName("MS949"),
            Charset.forName("CP949"),
            StandardCharsets.UTF_8,
            StandardCharsets.ISO_8859_1
        );

        try {
            Files.walk(downloadDir)
                .filter(path -> Files.isRegularFile(path) && isZipFile(path) && !path.toString().endsWith(".txt"))
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
                            String entryName = zipEntry.getName();

                            if (entryName.endsWith(".txt")) return;

                            Path outputPath = downloadDir.resolve(entryName);

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

    private boolean isZipFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] signature = new byte[4];
            if (is.read(signature) == 4) {
                return signature[0] == 'P' && signature[1] == 'K' && signature[2] == 3 && signature[3] == 4;
            }
        } catch (IOException e) {
            log.warn("ZIP 파일 여부 확인 실패: {}", path.getFileName(), e);
        }
        return false;
    }
}