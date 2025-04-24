package com.sangchu.preprocess.etl.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StoreRequestDto {

    private String storeId;
    
    private String storeNm;
    
    private String branchNm;

    private String largeCatCd;

    private String largeCatNm;

    private String midCatCd;

    private String midCatNm;

    private String smallCatCd;

    private String smallCatNm;

    private String ksicCd;

    private String ksicNm;

    private String sidoCd;

    private String sidoNm;

    private String sggCd;

    private String sggNm;

    private String hDongCd;

    private String hDongNm;

    private String bDongCd;

    private String bDongNm;

    private String lotNoCd;

    private String landDivCd;

    private String landDivNm;

    private String lotMainNo;

    private String lotSubNo;

    private String lotAddr;

    private String roadCd;

    private String roadNm;

    private String bldgMainNo;

    private String bldgSubNo;

    private String bldgMgmtNo;

    private String bldgNm;

    private String roadAddr;

    private String oldZipCd;

    private String newZipCd;

    private String block;

    private String floor;

    private String room;

    private BigDecimal coordX;

    private BigDecimal coordY;
}