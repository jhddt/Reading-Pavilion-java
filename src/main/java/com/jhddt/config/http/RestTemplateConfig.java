package com.jhddt.config.http;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 通用 RestTemplate（用于 OCR 等服务）
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 连接超时 10 秒
        factory.setReadTimeout(30000);    // 读取超时 30 秒
        return new RestTemplate(factory);
    }

    /**
     * DeepSeek API 专用 RestTemplate（设置更长的超时时间，因为批改作文可能需要较长时间）
     */
    @Bean(name = "deepSeekRestTemplate")
    public RestTemplate deepSeekRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);  // 连接超时 15 秒
        factory.setReadTimeout(120000);    // 读取超时 120 秒（2分钟），足够处理长文本批改
        return new RestTemplate(factory);
    }
}
