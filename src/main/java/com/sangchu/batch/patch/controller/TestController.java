package com.sangchu.batch.patch.controller;

import com.sangchu.batch.preprocess.service.PreprocessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final PreprocessService preprocessService;

    @GetMapping("/Test")
    public void Test() {
        preprocessService.indexAll();
    }
}
