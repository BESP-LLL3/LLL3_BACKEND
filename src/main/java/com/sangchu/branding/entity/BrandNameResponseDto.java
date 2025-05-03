package com.sangchu.branding.entity;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BrandNameResponseDto {

	private String brandName;
	private String comment;
}
