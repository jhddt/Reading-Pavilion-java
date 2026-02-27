package com.jhddt.module.essay.service;

/**
 * 文档解析服务
 */
public interface DocumentParserService {

    /**
     * 解析文档内容
     * @param documentPath 文档路径
     * @return 解析出的文本
     */
    String parseDocument(String documentPath);
}
