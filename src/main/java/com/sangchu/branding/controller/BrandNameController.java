package com.sangchu.branding.controller;

import com.sangchu.branding.entity.BrandNameRequestDto;
import com.sangchu.branding.entity.BrandNameResponseDto;
import com.sangchu.branding.service.OpenAiService;
import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.mapper.ResponseMapper;
import com.sangchu.global.response.BaseResponse;
import com.sangchu.global.util.statuscode.ApiStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class BrandNameController {
    private final OpenAiService openAiService;

    @PostMapping("/brand")
    public ResponseEntity<BaseResponse<List<BrandNameResponseDto>>> generateBrandName(@RequestBody BrandNameRequestDto brandNameRequestDto) {
        try {
            List<BrandNameResponseDto> response = openAiService.getBrandName(brandNameRequestDto).get(); // get()을 호출해 응답을 동기적으로 기다립니다.
            return ResponseMapper.successOf(ApiStatus._OK, response, BrandNameController.class);
        } catch (Exception e) {
            throw new CustomException(ApiStatus._BRANDING_FAIL);
        }
    }
}