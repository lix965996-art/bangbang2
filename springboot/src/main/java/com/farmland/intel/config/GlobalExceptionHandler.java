package com.farmland.intel.config;

import com.farmland.intel.common.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception e, HttpServletRequest request) {
        // 记录错误日志
        logger.error("系统异常: URL={}, Method={}, Error={}",
            request.getRequestURL(),
            request.getMethod(),
            e.getMessage(),
            e);

        // 返回统一的错误响应
        return ApiResponse.error("系统内部错误，请联系管理员");
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<String> handleBusinessException(BusinessException e, HttpServletRequest request) {
        logger.warn("业务异常: URL={}, Method={}, Code={}, Message={}",
            request.getRequestURL(),
            request.getMethod(),
            e.getCode(),
            e.getMessage());

        return ApiResponse.error(e.getMessage());
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<String> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        logger.warn("参数验证异常: URL={}, Method={}, Message={}",
            request.getRequestURL(),
            request.getMethod(),
            e.getMessage());

        return ApiResponse.error("参数验证失败: " + e.getMessage());
    }
}

/**
 * 业务异常类
 */
class BusinessException extends RuntimeException {
    private String code;
    private String message;

    public BusinessException(String message) {
        super(message);
        this.message = message;
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}