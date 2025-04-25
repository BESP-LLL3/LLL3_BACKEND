package com.sangchu.patent.service;

import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatentService {
    @Value("${kipris.service-key}")
    private String serviceKey;

    @Value("${kipris.request-url}")
    private String requestUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Boolean checkDuplicated(String storeNm) throws URISyntaxException, IOException, ParserConfigurationException, SAXException {
        try {
            String url = requestUrl + "?word=" + URLEncoder.encode(storeNm, StandardCharsets.UTF_8.toString()) + "&ServiceKey=" + serviceKey;
            ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

            if (response.getStatusCode() != HttpStatus.OK) return false;

            String xml = response.getBody();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            String totalCount = document.getElementsByTagName("totalCount").item(0).getTextContent();

            log.info("totalCount: " + totalCount);

            return Integer.parseInt(totalCount) > 0;
        } catch (Exception e) {
            throw new CustomException(ApiStatus._PATENT_CHECK_FAIL);
        }
    }
}
