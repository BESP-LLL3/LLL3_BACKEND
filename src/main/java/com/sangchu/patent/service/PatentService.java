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


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;
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

	public Boolean checkDuplicated(String storeNm) {
		try {
			String url =
				requestUrl + "?trademarkName=" + URLEncoder.encode(storeNm, StandardCharsets.UTF_8) + "&accessKey="
					+ serviceKey;
			ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

			if (response.getStatusCode() != HttpStatus.OK)
				return false;

			String xml = response.getBody();

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			Document document = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

			String totalCountText = document.getElementsByTagName("TotalSearchCount").item(0).getTextContent();
			NodeList applicationStatusNodeList = document.getElementsByTagName("ApplicationStatus");
			boolean patentCheck = false;
			for (int i = 0; i < applicationStatusNodeList.getLength(); i++) {
				String applicationStatus = applicationStatusNodeList.item(i).getTextContent();
				if ("등록".equals(applicationStatus) || "출원".equals(applicationStatus)) {
					patentCheck = true;
					break;
				}
			}
			return patentCheck || Integer.parseInt(totalCountText) > 0;
		} catch (Exception e) {
			throw new CustomException(ApiStatus._PATENT_CHECK_FAIL);
		}
	}
}
