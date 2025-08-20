package com.wu.wuaicode.ai;

import cn.hutool.core.io.FileUtil;
import com.wu.wuaicode.ai.model.HtmlCodeResult;
import com.wu.wuaicode.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult htmlCode = aiCodeGeneratorService.generateHtmlCode("给我创建一个关于编程的博客网站，代码不超过30行");
        System.out.println(htmlCode);
        Assertions.assertNotNull(htmlCode);
    }

    @Test
    void generateMultFileCode() {
        MultiFileCodeResult MultFileCode = aiCodeGeneratorService.generateMultFileCode("给我创建一个关于分享钓鱼的网站，代码不超过60行");
        System.out.println(MultFileCode);
        Assertions.assertNotNull(MultFileCode);
    }

}