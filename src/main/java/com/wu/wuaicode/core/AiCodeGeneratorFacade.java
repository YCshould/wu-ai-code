package com.wu.wuaicode.core;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wu.wuaicode.ai.AiCodeGeneratorService;
import com.wu.wuaicode.ai.AiGeneratorServiceFactory;
import com.wu.wuaicode.ai.model.HtmlCodeResult;
import com.wu.wuaicode.ai.model.MultiFileCodeResult;
import com.wu.wuaicode.ai.model.message.AiResponseMessage;
import com.wu.wuaicode.ai.model.message.ToolExecutedMessage;
import com.wu.wuaicode.ai.model.message.ToolRequestMessage;
import com.wu.wuaicode.constant.AppConstant;
import com.wu.wuaicode.core.builder.VueProjectBuilder;
import com.wu.wuaicode.core.parser.CodeParserExetcutor;
import com.wu.wuaicode.core.saver.CodeFileSaveExecutor;
import com.wu.wuaicode.exception.BusinessException;
import com.wu.wuaicode.exception.ErrorCode;
import com.wu.wuaicode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 门面模式，对外提供服务
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {


    @Resource
    private AiGeneratorServiceFactory generatorServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 非流式调用
     * 后续只调用这一个方法即可实现调用ai并保存文件
     * 普通ai模型
     * @param userMessage
     * @param codeGenType
     * @return
     */
    public File generateCodeAndSave(String userMessage, CodeGenTypeEnum codeGenType,Long appId){

        AiCodeGeneratorService aiCodeGeneratorService = generatorServiceFactory.getAiCodeGeneratorService(appId);

        if(codeGenType==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"未知的代码生成类型");
        }
        return switch (codeGenType){
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield  CodeFileSaveExecutor.codeFileSaveExecutor(htmlCodeResult,CodeGenTypeEnum.HTML,appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultFileCode(userMessage);
                yield  CodeFileSaveExecutor.codeFileSaveExecutor(multiFileCodeResult,CodeGenTypeEnum.MULTI_FILE,appId);
            }
            default -> {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"未知的代码生成类型");
            }
        };
    }

//    private File generateAndSaveHtml(String userMessage) {
//        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
//        return FileSave.saveHtml(htmlCodeResult);
//    }
//
//    private File generateAndSaveMultiFile(String userMessage) {
//        MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultFileCode(userMessage);
//        return FileSave.saveMultiFileCodeResult(multiFileCodeResult);
//    }

    /**
     * 流式调用，返回Flux<String>，可以对其进行流式处理
     * @param userMessage
     * @param codeGenType
     * @return
     */
    public Flux<String> generateCodeAndSaveStream(String userMessage, CodeGenTypeEnum codeGenType,Long appId){

        AiCodeGeneratorService aiCodeGeneratorService = generatorServiceFactory.getAiCodeGeneratorService(appId,codeGenType);

        if(codeGenType==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"未知的代码生成类型");
        }
        return switch (codeGenType){
            case HTML -> {
                Flux<String> htmlStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield  generateAndSaveStream(htmlStream, CodeGenTypeEnum.HTML,appId);
            }
            case MULTI_FILE -> {
                Flux<String> multFileStream = aiCodeGeneratorService.generateMultFileCodeStream(userMessage);
                yield  generateAndSaveStream(multFileStream, CodeGenTypeEnum.MULTI_FILE,appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield  processTokenStream(tokenStream);
            }
            default -> {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"未知的代码生成类型");
            }
        };
    }

    private Flux<String> generateAndSaveStream(Flux<String> userMessageStream,CodeGenTypeEnum codeGenType,Long appId) {

        StringBuilder stringBuilder = new StringBuilder();
        return userMessageStream.doOnNext(chunk->{
            //每拿到一块代码块就要用字符拼接器对其进行拼接
            stringBuilder.append(chunk);
        }).doOnComplete(()->{
            try {
                //当所有代码块都拿到后，用代码解析器把一串的代码字符解析为结构化的HTML代码
                String codeParing = stringBuilder.toString();
                Object parserExecutor = CodeParserExetcutor.parserExecutor(codeParing, codeGenType);
                File file = CodeFileSaveExecutor.codeFileSaveExecutor(parserExecutor, codeGenType,appId);
                log.info("MultiFileCode文件保存成功,路径为:{}",file.getAbsolutePath());
            } catch (Exception e) {
                log.error("解析MultiFileCode代码出错,保存MultiFileCode文件失败");
            }
        });
//        return userMessageStream;
//        这意味着调用方将接收到的是未经过处理的原始流
//        您添加的doOnNext和doOnComplete操作实际上只在方法内部创建了新的处理链
//        但没有被实际订阅或返回，因此永远不会执行
//        所以不能这样写
    }

//    private Flux<String> generateAndSaveHtmlStream(String userMessage) {
//        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
//        StringBuilder stringBuilder = new StringBuilder();
//        result.doOnNext(chunk->{
//            //每拿到一块代码块就要用字符拼接器对其进行拼接
//            stringBuilder.append(chunk);
//        }).doOnComplete(()->{
//            try {
//                //当所有代码块都拿到后，用代码解析器把一串的代码字符解析为结构化的HTML代码
//                String codeParing = stringBuilder.toString();
//                HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(codeParing);
//                File file = FileSave.saveHtml(htmlCodeResult);
//                log.info("HTML文件保存成功,路径为:{}",file.getAbsolutePath());
//            } catch (Exception e) {
//                log.error("解析HTML代码出错,保存HTML文件失败");
//            }
//        });
//        return result;
//    }
    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    // 修改 AiCodeGeneratorFacade.java 中的 processTokenStream 方法

    // 引入必要的包

// ... 在类中找到 processTokenStream 方法并替换 ...

    /**
     * 将 TokenStream 转换为 Flux<String>，并增加熔断机制
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        // 1. 定义一个“结束信号”异常，用于强制打断流
        class EndSignalException extends RuntimeException {
            public EndSignalException(String message) { super(message); }
        }
        return Flux.<String>create(sink -> {
            // 1. 定义熔断计数器：防止 AI 无限生成文件
            // 根据你的 Prompt，Vue 项目大约 10-12 个文件，我们设 12 个作为安全上限
            AtomicInteger fileWriteCount = new AtomicInteger(0);
            final int MAX_FILE_COUNT = 15;
            // 2. 【核心】全局停止标志位 (默认为 false)
            // 使用 AtomicBoolean 保证在不同回调中的可见性
            AtomicBoolean isForcedStop = new AtomicBoolean(false);
            Set<String> writtenFiles = new ConcurrentHashSet<>();

            tokenStream.onPartialResponse((String partialResponse) -> {
                        // 2. 【关键词熔断】：如果 AI 说了“生成完毕”，直接强制结束
                        // 配合 Prompt 里的约束使用
                        if (partialResponse.contains("项目生成完毕") || partialResponse.contains("done")) {
                            log.info("检测到结束关键词，强制结束流");
                            sink.complete();
                            isForcedStop.set(true);
                            sink.complete();
                            throw new EndSignalException("NormalFinish");
                        }
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        // 如果已熔断，直接返回，不再执行后续计数逻辑
                        if (isForcedStop.get()) {
                            return;
                        }
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        // 【新增】如果已经标记停止，直接忽略后续的任何工具调用，不再处理
                        if (isForcedStop.get()) {
                            return;
                        }
                        // 3. 【数量熔断】：监控“写入文件”工具的调用次数
                        String toolName = toolExecution.request().name();
                        // 假设你的工具名包含 "Write" 或 "write" (如 FileWriteTool)
                        if (toolName.toLowerCase().contains("write")) {
                            int current = fileWriteCount.incrementAndGet();
                            // --- 【新增】重复文件检测逻辑 ---
                            String args = toolExecution.request().arguments();
                            // 简单解析出文件名，假设 args 里包含 "relativeFilePath": "src/App.vue"
                            // 这里用简单的字符串匹配，或者你可以解析 JSON
                            String filePath = null;
                            try {
                                JSONObject json = JSONUtil.parseObj(args);
                                filePath = json.getStr("relativeFilePath");
                            } catch (Exception e) {
                                // 解析失败就算了
                            }

                            // 如果这个文件之前已经写过了 -> 判定为死循环 -> 立即杀掉
                            if (filePath != null && writtenFiles.contains(filePath)) {
                                log.warn("【严重】检测到 AI 正在重复写入文件: {}，判定为死循环，立即终止！", filePath);

                                isForcedStop.set(true);
                                // 发送结束信号给前端
                                AiResponseMessage msg = new AiResponseMessage("\n\n> 系统监控：检测到重复生成行为，已强制完成构建。\n\n");
                                sink.next(JSONUtil.toJsonStr(msg));
                                sink.complete();
                                throw new EndSignalException("DuplicateFile");
                            }

                            // 记录这个文件已写入
                            if (filePath != null) {
                                writtenFiles.add(filePath);
                            }
                            log.info("AI 正在写入第 {} 个文件", current);

                            if (current > MAX_FILE_COUNT) {
                                log.warn("【严重】检测到 AI 死循环（文件数 > {}），强制熔断截断流！", MAX_FILE_COUNT);
                                // A. 立即设置停止标志！关上大门！
                                isForcedStop.set(true);
                                // 【修改点 2】：先给前端发一条提示消息，让用户知道发生了什么
                                AiResponseMessage msg = new AiResponseMessage("\n\n> 系统提示：检测到生成文件数量较多，已强制截断生成流，即将进入构建阶段...\n\n");
                                sink.next(JSONUtil.toJsonStr(msg));
                                AiResponseMessage msg_1 = new AiResponseMessage("\n\n> 项目构建即将完成，请稍等页面刷新\n\n");
                                sink.next(JSONUtil.toJsonStr(msg_1));
                                sink.complete();
                                throw new EndSignalException("MaxCountLimit");
                            }
                        }

                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        if (!isForcedStop.get()) {
                            sink.complete();
                        }
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        // 如果是我们自己抛出的结束信号，直接忽略，不要打印错误日志
                        if (error instanceof EndSignalException) {
                            return;
                        }
                        log.error("流式生成发生未知异常", error);
                        sink.error(error);
                    })
                    .start();
        }).onErrorResume(e -> {
            if (e instanceof EndSignalException || e.getMessage().equals("EndSignalException")) {
                log.info("流处理已通过熔断机制正常终止: {}", e.getMessage());
                // 技巧：声明一个明确类型的变量，然后再 return，编译器就不会晕了
                return Flux.empty();
            }
            return Flux.error(e);
        });
    }
//    private Flux<String> processTokenStream(TokenStream tokenStream) {
//        return Flux.create(sink -> {
//            tokenStream.onPartialResponse((String partialResponse) -> {
//                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
//                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
//                    })
//                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
//                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
//                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
//                    })
//                    .onToolExecuted((ToolExecution toolExecution) -> {
//                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
//                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
//                    })
//                    .onCompleteResponse((ChatResponse response) -> {
//                        sink.complete();
//                    })
//                    .onError((Throwable error) -> {
//                        error.printStackTrace();
//                        sink.error(error);
//                    })
//                    .start();
//        });
//    }


}



