package com.sangchu.trend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KeywordInfo {
	private String keyword;
	private double relevance;
}