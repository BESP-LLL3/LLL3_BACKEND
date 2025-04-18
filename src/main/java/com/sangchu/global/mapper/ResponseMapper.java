package com.sangchu.global.mapper;


import org.springframework.http.ResponseEntity;

import com.sangchu.global.response.BaseResponse;
import com.sangchu.global.response.ErrorResponse;
import com.sangchu.global.response.SuccessResponse;
import com.sangchu.global.util.statuscode.ApiStatus;

public class ResponseMapper {

	public static <T> ResponseEntity<BaseResponse<T>> successOf(ApiStatus code, T data, Class<?> handleClass) {
		BaseResponse<T> response = new SuccessResponse<>(code.getCode(), code.getMessage(), data, handleClass);
		return ResponseEntity.status(code.getHttpStatus()).body(response);
	}
	public static <T> ResponseEntity<BaseResponse<T>> errorOf(ApiStatus code, Class<?> handleClass) {
		BaseResponse<T> response = new ErrorResponse<>(code.getCode(), code.getMessage(), handleClass);
		return ResponseEntity.status(code.getHttpStatus()).body(response);
	}

}
