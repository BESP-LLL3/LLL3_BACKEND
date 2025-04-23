package com.sangchu.preprocess.etl.mapper;

import com.sangchu.preprocess.etl.entity.Store;
import com.sangchu.preprocess.etl.entity.StoreRequestDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StoreMapper {

    public static Store toEntity(String crtrYm, StoreRequestDto storeRequestDto) {
        return Store.builder()
                .crtrYm(crtrYm)
                .storeId(storeRequestDto.getStoreId())
                .storeNm(storeRequestDto.getStoreNm())
                .branchNm(storeRequestDto.getBranchNm())
                .largeCatCd(storeRequestDto.getLargeCatCd())
                .largeCatNm(storeRequestDto.getLargeCatNm())
                .midCatCd(storeRequestDto.getMidCatCd())
                .midCatNm(storeRequestDto.getMidCatNm())
                .smallCatCd(storeRequestDto.getSmallCatCd())
                .smallCatNm(storeRequestDto.getSmallCatNm())
                .ksicCd(storeRequestDto.getKsicCd())
                .ksicNm(storeRequestDto.getKsicNm())
                .sidoCd(storeRequestDto.getSidoCd())
                .sidoNm(storeRequestDto.getSidoNm())
                .sggCd(storeRequestDto.getSggCd())
                .sggNm(storeRequestDto.getSggNm())
                .hDongCd(storeRequestDto.getHDongCd())
                .hDongNm(storeRequestDto.getHDongNm())
                .bDongCd(storeRequestDto.getBDongCd())
                .bDongNm(storeRequestDto.getBDongNm())
                .lotNoCd(storeRequestDto.getLotNoCd())
                .landDivCd(storeRequestDto.getLandDivCd())
                .landDivNm(storeRequestDto.getLandDivNm())
                .lotMainNo(storeRequestDto.getLotMainNo())
                .lotSubNo(storeRequestDto.getLotSubNo())
                .lotAddr(storeRequestDto.getLotAddr())
                .roadCd(storeRequestDto.getRoadCd())
                .roadNm(storeRequestDto.getRoadNm())
                .bldgMainNo(storeRequestDto.getBldgMainNo())
                .bldgSubNo(storeRequestDto.getBldgSubNo())
                .bldgMgmtNo(storeRequestDto.getBldgMgmtNo())
                .bldgNm(storeRequestDto.getBldgNm())
                .roadAddr(storeRequestDto.getRoadAddr())
                .oldZipCd(storeRequestDto.getOldZipCd())
                .newZipCd(storeRequestDto.getNewZipCd())
                .block(storeRequestDto.getBlock())
                .floor(storeRequestDto.getFloor())
                .room(storeRequestDto.getRoom())
                .coordX(storeRequestDto.getCoordX())
                .coordY(storeRequestDto.getCoordY())
                .build();
    }
}
