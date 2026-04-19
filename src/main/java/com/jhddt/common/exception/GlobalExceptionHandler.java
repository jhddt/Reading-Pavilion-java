package com.jhddt.common.exception;

import com.jhddt.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        
        // 提取友好的错误信息
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("OCR 服务调用失败")) {
                return Result.error("OCR 识别服务暂时不可用，请稍后重试");
            } else if (message.contains("文件上传失败")) {
                return Result.error("文件上传失败: " + extractErrorMessage(message));
            } else if (message.contains("文档处理失败")) {
                return Result.error("文档解析失败: " + extractErrorMessage(message));
            }
        }
        
        return Result.error("操作失败: " + (message != null ? message : "未知错误"));
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);  // 这会打印完整堆栈
        log.error("异常类型: {}", e.getClass().getName());
        log.error("异常消息: {}", e.getMessage());
        if (e.getCause() != null) {
            log.error("异常原因: {}", e.getCause().getMessage());
        }
        return Result.error("系统异常，请联系管理员");
    }

    /**
     * 提取简洁的错误信息
     */
    private String extractErrorMessage(String fullMessage) {
        if (fullMessage == null) {
            return "未知错误";
        }
        
        // 提取最后一个冒号后的内容
        int lastColon = fullMessage.lastIndexOf(":");
        if (lastColon > 0 && lastColon < fullMessage.length() - 1) {
            return fullMessage.substring(lastColon + 1).trim();
        }
        
        return fullMessage;
    }
}
