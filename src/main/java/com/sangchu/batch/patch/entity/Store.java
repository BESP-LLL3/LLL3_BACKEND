package com.sangchu.batch.patch.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Entity
public class Store {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 6, nullable = false)
	private String crtrYm;

	@Column(nullable = false)
	private String storeId;

	@Column(nullable = false)
	private String storeNm;

	private String branchNm;

	@Column(length = 2, nullable = false)
	private String largeCatCd;

	@Column(nullable = false)
	private String largeCatNm;

	@Column(length = 4, nullable = false)
	private String midCatCd;

	@Column(nullable = false)
	private String midCatNm;

	@Column(length = 6, nullable = false)
	private String smallCatCd;

	@Column(nullable = false)
	private String smallCatNm;

	@Column(length = 6)
	private String ksicCd;

	private String ksicNm;

	@Column(length = 2, nullable = false)
	private String sidoCd;

	@Column(nullable = false)
	private String sidoNm;

	@Column(length = 5, nullable = false)
	private String sggCd;

	@Column(nullable = false)
	private String sggNm;

	@Column(length = 10, nullable = false)
	private String hDongCd;

	@Column(nullable = false)
	private String hDongNm;

	@Column(length = 10, nullable = false)
	private String bDongCd;

	@Column(nullable = false)
	private String bDongNm;

	@Column(length = 20, nullable = false)
	private String lotNoCd;

	@Column(length = 2, nullable = false)
	private String landDivCd;

	@Column(nullable = false)
	private String landDivNm;

	@Column(nullable = false)
	private String lotMainNo;

	private String lotSubNo;

	@Column(nullable = false)
	private String lotAddr;

	@Column(length = 12, nullable = false)
	private String roadCd;

	@Column(nullable = false)
	private String roadNm;

	@Column(nullable = false)
	private String bldgMainNo;

	private String bldgSubNo;

	@Column(length = 25 , nullable = false)
	private String bldgMgmtNo;

	@Column(nullable = false)
	private String bldgNm;

	@Column(nullable = false)
	private String roadAddr;

	@Column(length = 6, nullable = false)
	private String oldZipCd;

	@Column(length = 4, nullable = false)
	private String newZipCd;

	@Column(length = 50)
	private String block;

	@Column(length = 50)
	private String floor;

	@Column(length = 50)
	private String room;

	@Column(length = 15, nullable = false)
	private String coordX;

	@Column(length = 15, nullable = false)
	private String coordY;

	@CreatedDate
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;
}