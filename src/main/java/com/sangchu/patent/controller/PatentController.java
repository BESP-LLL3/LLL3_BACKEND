package com.sangchu.patent.controller;

import com.sangchu.global.mapper.ResponseMapper;
import com.sangchu.global.response.BaseResponse;
import com.sangchu.global.util.statuscode.ApiStatus;
import com.sangchu.patent.service.PatentService;
import com.sangchu.prefer.entity.PreferNameCreateDto;
import com.sangchu.prefer.service.PreferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PatentController {
    private final PatentService patentService;
    private final PreferService preferService;

    @GetMapping("/patent")
    public ResponseEntity<BaseResponse<Boolean>> checkPatent(
            @RequestParam("keyword") String keyword,
            @RequestParam("custom") String custom,
            @RequestParam("storeNm") String storeNm
    ) {
        PreferNameCreateDto preferName = PreferNameCreateDto.builder().keyword(keyword).custom(custom).name(storeNm).build();
        preferService.createPreferName(preferName);
        Boolean response = patentService.checkDuplicated(storeNm);
        return ResponseMapper.successOf(ApiStatus._OK, response, PatentController.class);
    }
}
