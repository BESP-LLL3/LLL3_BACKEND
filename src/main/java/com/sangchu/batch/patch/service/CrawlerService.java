package com.sangchu.batch.patch.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameTranslateMappingStrategy;
import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.patch.entity.StoreRequestDto;
import com.sangchu.batch.patch.mapper.StoreMapper;
import com.sangchu.batch.patch.repository.StoreRepository;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.transaction.Transactional;
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

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {
    private final StoreRepository storeRepository;

    private static final Map<String, String> columnMapping = Map.ofEntries(
        //Map.entry("기준년월", "crtr_ym"),
        Map.entry("상가업소번호", "storeId"),
        Map.entry("상호명","storeNm"),
        Map.entry("지점명","branch_nm"),
        Map.entry("상권업종대분류코드", "largeCatCd"),
        Map.entry("상권업종대분류명", "largeCatNm"),
        Map.entry("상권업종중분류코드", "midCatCd"),
        Map.entry("상권업종중분류명", "midCatNm"),
        Map.entry("상권업종소분류코드", "smallCatCd"),
        Map.entry("상권업종소분류명", "smallCatNm"),
        Map.entry("표준산업분류코드", "ksicCd"),
        Map.entry("표준산업분류명", "ksicNm"),
        Map.entry("시도코드", "sidoCd"),
        Map.entry("시도명", "sidoNm"),
        Map.entry("시군구코드", "sggCd"),
        Map.entry("시군구명", "sggNm"),
        Map.entry("행정동코드", "hDongCd"),
        Map.entry("행정동명", "hDongNm"),
        Map.entry("법정동코드", "bDongCd"),
        Map.entry("법정동명", "bDongNm"),
        Map.entry("지번코드", "lotNoCd"),
        Map.entry("대지구분코드", "landDivCd"),
        Map.entry("대지구분명", "landDivNm"),
        Map.entry("지번본번지", "lotMainNo"),
        Map.entry("지번부번지", "lotSubNo"),
        Map.entry("지번주소", "lotAddr"),
        Map.entry("도로명코드", "roadCd"),
        Map.entry("도로명", "roadNm"),
        Map.entry("건물본번", "bldgMainNo"),
        Map.entry("건물부번", "bldgSubNo"),
        Map.entry("건물관리번호", "bldgMgmtNo"),
        Map.entry("건물명", "bldgNm"),
        Map.entry("도로명주소", "roadAddr"),
        Map.entry("구우편번호", "oldZipCd"),
        Map.entry("신우편번호", "newZipCd"),
        Map.entry("건물동", "block"),
        Map.entry("건물층", "floor"),
        Map.entry("호", "room"),
        Map.entry("경도", "coordX"),
        Map.entry("위도", "coordY")
    );

    // csv 파일을 가져오는 메서드
    public void getStoreCsvToMySql() throws IOException {
//        crwaling();
//        fileUnZip();
        importAllCsvFromFolder();
    }

    /**
    * 공공데이터포털에서 소상공인진흥공단 상가(상권)정보를 찾아
    * 다운받은 압축 파일을 resources/data에 저장
    */
    private void crwaling() {
        WebDriverManager.chromedriver().setup();

        Path resourcePath = Paths.get("src/main/resources/data").toAbsolutePath();
        log.info("resourcePath = " + resourcePath);

        resetDirectory(resourcePath);

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

    private void resetDirectory(Path downloadDir) {
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

        try {
            Files.walk(downloadDir)
                .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".zip"))
                .forEach(zipFilePath -> {
                    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
                        ZipEntry zipEntry;
                        while ((zipEntry = zis.getNextEntry()) != null) {
                            Path outputPath = downloadDir.resolve(zipEntry.getName());

                            // 디렉토리인 경우 디렉토리 생성
                            if (zipEntry.isDirectory()) {
                                Files.createDirectories(outputPath);
                            } else {
                                // 부모 디렉토리 없을 경우 생성
                                if (outputPath.getParent() != null) {
                                    Files.createDirectories(outputPath.getParent());
                                }

                                // 파일 복사
                                try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(outputPath))) {
                                    byte[] buffer = new byte[1024];
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        bos.write(buffer, 0, len);
                                    }
                                }
                            }
                            zis.closeEntry();
                        }

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

    @Transactional
    public void importAllCsvFromFolder() throws IOException {
        Path dirPath = Paths.get("src/main/resources/data");
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:src/main/resources/data/*.csv");

        List<Store> allStores = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.csv")) {
            for (Path path : stream) {
                if (matcher.matches(path)) {
                    log.info("Importing file: " + path.getFileName());
                    List<Store> storeList = readCsvFile(path);
                    allStores.addAll(storeList);
                }
            }
        }

//        storeRepository.saveAll(allStores);
        log.info("총 저장된 상점 수: " + allStores.size());
    }

    private List<Store> readCsvFile(Path path) {
        try {
            HeaderColumnNameTranslateMappingStrategy<StoreRequestDto> strategy = new HeaderColumnNameTranslateMappingStrategy<>();
            strategy.setType(StoreRequestDto.class);
            strategy.setColumnMapping(columnMapping);

            List<StoreRequestDto> stores = new CsvToBeanBuilder<StoreRequestDto>(new FileReader(path.toFile()))
                            .withMappingStrategy(strategy)
//                            .withSkipLines(1)
                            .withSeparator(',')
                            .build()
                            .parse();
            for (StoreRequestDto store : stores) {
                log.info("log : {}", store.getBldgMainNo());
            }
            // 기준년월 주입
            String crtrYm = extractCrtrYmFromFileName(path);
            log.info("csv 파싱 완료", path.getFileName(), crtrYm);

            return stores.stream()
                   .map(store -> StoreMapper.toEntity(crtrYm, store))
                   .toList();

        } catch (Exception e) {
            log.error("CSV 파싱 실패: " + path.getFileName() + " → " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private String extractCrtrYmFromFileName(Path path) {
        String filename = path.getFileName().toString();
        String[] parts = filename.split("_");
        String lastPart = parts[parts.length - 1];
        return lastPart.replace(".csv", "");
    }

}
