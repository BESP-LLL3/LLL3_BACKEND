package com.sangchu.global.exception.custom;

import com.sangchu.global.util.statuscode.ApiStatus;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
	private final ApiStatus status;

	public CustomException(ApiStatus status) {
		super(status.getMessage());
		this.status = status;
	}

	public CustomException(ApiStatus status, String message) {
		super(message);
		this.status = status;
	}

}
