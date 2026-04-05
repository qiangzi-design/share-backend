package com.share.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常
 * 用于统一抛出可预期的业务错误，并携带HTTP状态与业务错误码
 */
public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus httpStatus;

    public BusinessException(HttpStatus httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
