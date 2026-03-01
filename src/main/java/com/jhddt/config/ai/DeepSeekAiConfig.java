package com.jhddt.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringAI DeepSeek 配置类
 * 使用 OpenAI 兼容接口（DeepSeek API 兼容 OpenAI 格式）
 * SpringAI 会自动从 application.yml 读取配置并创建 ChatModel
 */
@Configuration
public class DeepSeekAiConfig {

    /**
     * 配置 ChatClient，用于简化 AI 调用
     * SpringAI 会自动注入配置好的 ChatModel
     */
    @Bean
    public ChatClient deepSeekChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一位经验丰富的语文老师，擅长批改学生作文。")
                .build();
    }
}
