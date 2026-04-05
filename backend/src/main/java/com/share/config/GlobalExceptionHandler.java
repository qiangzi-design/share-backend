package com.share.config;

import com.share.dto.ApiResponse;
import com.share.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        HttpStatus status = ex.getHttpStatus() == null ? HttpStatus.BAD_REQUEST : ex.getHttpStatus();
        int code = ex.getCode() > 0 ? ex.getCode() : status.value();
        log.warn("Business exception [{} {}] code={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                code,
                ex.getMessage());
        return ResponseEntity.status(status).body(ApiResponse.error(code, ex.getMessage()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            MaxUploadSizeExceededException.class,
            IllegalArgumentException.class
    })
    public ApiResponse handleBadRequest(Exception ex, HttpServletRequest request) {
        String message = "请求参数不合法";
        if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            FieldError fieldError = methodArgumentNotValidException.getBindingResult().getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        } else if (ex instanceof MaxUploadSizeExceededException) {
            message = "上传文件超过大小限制（最大5MB）";
        } else if (ex instanceof BindException bindException) {
            FieldError fieldError = bindException.getBindingResult().getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        } else if (ex instanceof ConstraintViolationException constraintViolationException && !constraintViolationException.getConstraintViolations().isEmpty()) {
            message = constraintViolationException.getConstraintViolations().iterator().next().getMessage();
        } else if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            message = ex.getMessage();
        }
        log.warn("Bad request [{} {}] type={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                message);
        return ApiResponse.error(400, message);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse handleUnauthorized(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Unauthorized [{} {}] message={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());
        return ApiResponse.error(401, ex.getMessage() == null ? "未授权访问" : ex.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse handleForbidden(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Forbidden [{} {}] message={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());
        return ApiResponse.error(403, "无权访问该资源");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception [{} {}]", request.getMethod(), request.getRequestURI(), ex);
        return ApiResponse.error(500, "服务器内部错误");
    }
}
