package com.jhddt.module.essay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 纯文本创建作文请求
 */
@Schema(description = "纯文本创建作文请求")
@Data
public class CreateTextEssayRequest {

    @Schema(description = "作文标题", example = "我的假期", required = true)
    private String title;

    @Schema(description = "作文内容", example = "这是作文正文内容...", required = true)
    private String content;

    @Schema(description = "美化后的作文内容", example = "这是经过润色后的作文正文内容...")
    private String beautifiedContent;
}
