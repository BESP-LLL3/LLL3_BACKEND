package com.sangchu.batch.patch.mapper;

import com.sangchu.batch.patch.entity.Store;
import com.sangchu.batch.patch.entity.StoreRequestDto;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class StoreMapper {

    public static Store toEntity(String crtrYm, StoreRequestDto storeRequestDto) {
        log.info("storeRequestDto value1 : {}", storeRequestDto.getCoordX());
        log.info("storeRequestDto value2 : {}", storeRequestDto.getCoordY());
        return Store.builder()
               .crtrYm(crtrYm)
               .storeId(safe(storeRequestDto.getStoreId()))
               .storeNm(safe(storeRequestDto.getStoreNm()))
               .branchNm(safe(storeRequestDto.getBranchNm()))
               .largeCatCd(safe(storeRequestDto.getLargeCatCd()))
               .largeCatNm(safe(storeRequestDto.getLargeCatNm()))
               .midCatCd(safe(storeRequestDto.getMidCatCd()))
               .midCatNm(safe(storeRequestDto.getMidCatNm()))
               .smallCatCd(safe(storeRequestDto.getSmallCatCd()))
               .smallCatNm(safe(storeRequestDto.getSmallCatNm()))
               .ksicCd(safe(storeRequestDto.getKsicCd()))
               .ksicNm(safe(storeRequestDto.getKsicNm()))
               .sidoCd(safe(storeRequestDto.getSidoCd()))
               .sidoNm(safe(storeRequestDto.getSidoNm()))
               .sggCd(safe(storeRequestDto.getSggCd()))
               .sggNm(safe(storeRequestDto.getSggNm()))
               .hDongCd(safe(storeRequestDto.getHDongCd()))
               .hDongNm(safe(storeRequestDto.getHDongNm()))
               .bDongCd(safe(storeRequestDto.getBDongCd()))
               .bDongNm(safe(storeRequestDto.getBDongNm()))
               .lotNoCd(safe(storeRequestDto.getLotNoCd()))
               .landDivCd(safe(storeRequestDto.getLandDivCd()))
               .landDivNm(safe(storeRequestDto.getLandDivNm()))
               .lotMainNo(safe(storeRequestDto.getLotMainNo()))
               .lotSubNo(safe(storeRequestDto.getLotSubNo()))
               .lotAddr(safe(storeRequestDto.getLotAddr()))
               .roadCd(safe(storeRequestDto.getRoadCd()))
               .roadNm(safe(storeRequestDto.getRoadNm()))
               .bldgMainNo(safe(storeRequestDto.getBldgMainNo()))
               .bldgSubNo(safe(storeRequestDto.getBldgSubNo()))
               .bldgMgmtNo(safe(storeRequestDto.getBldgMgmtNo()))
               .bldgNm(safe(storeRequestDto.getBldgNm()))
               .roadAddr(safe(storeRequestDto.getRoadAddr()))
               .oldZipCd(safe(storeRequestDto.getOldZipCd()))
               .newZipCd(safe(storeRequestDto.getNewZipCd()))
               .block(safe(storeRequestDto.getBlock()))
               .floor(safe(storeRequestDto.getFloor()))
               .room(safe(storeRequestDto.getRoom()))
               .coordX(safe(storeRequestDto.getCoordX()))
               .coordY(safe(storeRequestDto.getCoordY()))
               .build();
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }

    public static BigDecimal safe(BigDecimal value) {
        return (value == null || value.equals(BigDecimal.ZERO)) ? BigDecimal.valueOf(0) : value;
    }
}
