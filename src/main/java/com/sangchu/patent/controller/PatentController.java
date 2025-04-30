package com.sangchu.patent.controller;

import com.sangchu.global.mapper.ResponseMapper;
import com.sangchu.global.response.BaseResponse;
import com.sangchu.global.util.statuscode.ApiStatus;
import com.sangchu.patent.service.PatentService;
import com.sangchu.prefer.entity.PreferNameCreateDto;
import com.sangchu.prefer.service.PreferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PatentController {
    private final PatentService patentService;
    private final PreferService preferService;

    @GetMapping("/patent")
    public ResponseEntity<BaseResponse<Boolean>> checkPatent(
            @Param("keyword") String keyword,
            @Param("custom") String custom,
            @Param("storeNm") String storeNm
    ) throws URISyntaxException, IOException, ParserConfigurationException, SAXException {
        PreferNameCreateDto preferName = PreferNameCreateDto.builder().keyword(keyword).custom(custom).name(storeNm).build();
        preferService.createPreferName(preferName);
        Boolean response = patentService.checkDuplicated(storeNm);
        return ResponseMapper.successOf(ApiStatus._OK, response, PatentController.class);
    }
}
