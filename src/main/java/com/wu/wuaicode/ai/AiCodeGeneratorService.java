package com.wu.wuaicode.ai;

import com.wu.wuaicode.ai.model.HtmlCodeResult;
import com.wu.wuaicode.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    /**
     * Ai生成Html代码
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * Ai生成Html代码
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/mult-file-system-prompt.txt")
    MultiFileCodeResult generateMultFileCode(String userMessage);

    /**
     * Ai生成Html代码
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * Ai生成Html代码
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/mult-file-system-prompt.txt")
    Flux<String> generateMultFileCodeStream(String userMessage);

    /**
     * Ai生成Vue-Project代码
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/vue-project-system-prompt.txt")
    TokenStream generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);
}
