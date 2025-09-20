package com.deepsearch.exception;

/**
 * 请求参数错误异常
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}