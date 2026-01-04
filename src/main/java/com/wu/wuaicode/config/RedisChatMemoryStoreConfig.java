package com.wu.wuaicode.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {

    private String host;

    private int port;

    private String password;

    private long ttl;

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        // 注意：langchain4j 1.1.0-beta7 版本的 builder 不支持自定义连接池配置
        // 需要通过修改 Redis 服务器配置来避免 Connection reset 错误
        // 在 redis.conf 中设置: timeout 0 (禁用空闲连接超时)
        return RedisChatMemoryStore.builder()
                .ttl(ttl)
                .host(host)
                .password(password)
                .port(port)
                .build();
    }
}
