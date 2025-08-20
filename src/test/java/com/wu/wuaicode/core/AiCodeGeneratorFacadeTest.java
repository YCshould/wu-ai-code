package com.wu.wuaicode.core;

import com.wu.wuaicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateCodeAndSave() {
        File file = aiCodeGeneratorFacade.generateCodeAndSave("给我创建一个关于分享钓鱼的网站，代码不超过60行", CodeGenTypeEnum.MULTI_FILE);
    }

    @Test
    void generateCodeAndSaveStream() {
        Flux<String> stringFlux = aiCodeGeneratorFacade.generateCodeAndSaveStream("给我创建一个关于分享钓鱼的网站，代码不超过60行", CodeGenTypeEnum.MULTI_FILE);
        List<String> block = stringFlux.collectList().block();
        Assertions.assertNotNull(block);
        String join = String.join("", block);
        System.out.println(join);
    }
}