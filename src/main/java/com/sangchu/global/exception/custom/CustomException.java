package com.sangchu.global.exception.custom;

import com.sangchu.global.util.statuscode.ApiStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomException extends RuntimeException {
	private final ApiStatus status;

}
