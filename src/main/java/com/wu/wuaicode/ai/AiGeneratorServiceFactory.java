package com.wu.wuaicode.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wu.wuaicode.ai.tools.*;
import com.wu.wuaicode.exception.BusinessException;
import com.wu.wuaicode.exception.ErrorCode;
import com.wu.wuaicode.model.enums.CodeGenTypeEnum;
import com.wu.wuaicode.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class AiGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel openAiStreamingChatModel;

    @Resource
    private StreamingChatModel reasoningStreamingChatModel;  // 这个bean是单独配置，用于流式推理模型

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;


    /**
     * 本地缓存
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 从Caffeine缓存中获取 AI 服务实例
     * 如果缓存中没有，则通过函数式接口创建新的 AI 服务实例并缓存
     * 如果缓存中有，则根据appid直接返回缓存中的实例
     * 这个也是会又过期时间的，上面注释就有
     * @param appId
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }
    // 1=> AiService1,对话记忆 id=1
    // 2=> AiService2,对话记忆 id=2

    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 根据 appId 获取服务
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 这个代码里面加载历史消息的到对话记忆里面前已经把原先的存在redis的chatMemory清空了
        int historyCount = chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            // Vue 项目生成使用推理模型
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(toolManager.getAllTools())
                    .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                            toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                    ))
                    .build();
            // HTML 和多文件生成使用默认模型
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(openAiStreamingChatModel)
                    .chatMemory(chatMemory)
                    .build();
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }
    /**
     * 构建缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }


    /**
     * 后续每个对话创建的都是不同的服务实例，所以这里可以暂时不要
     * @return
     */
//    @Bean
//    public AiCodeGeneratorService aiCodeGeneratorService() {
//        return getAiCodeGeneratorService(0L);
//    }


}





