package com.sangchu.prefer.service;

import com.sangchu.global.exception.custom.CustomException;
import com.sangchu.global.util.statuscode.ApiStatus;
import com.sangchu.prefer.entity.PreferName;
import com.sangchu.prefer.entity.PreferNameCreateDto;
import com.sangchu.prefer.mapper.PreferMapper;
import com.sangchu.prefer.repository.PreferNameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PreferService {

    private final PreferNameRepository preferNameRepository;

    public void createPreferName(PreferNameCreateDto preferNameCreateDto) {

        try {
            PreferName preferName = PreferMapper.toEntity(preferNameCreateDto);
            preferNameRepository.save(preferName);
        } catch (Exception e) {
            throw new CustomException(ApiStatus._PREFER_SAVE_FAIL, e.getMessage());
        }
    }
}
