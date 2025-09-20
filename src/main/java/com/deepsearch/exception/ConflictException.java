package com.deepsearch.exception;

/**
 * 冲突异常（如用户名已存在）
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}