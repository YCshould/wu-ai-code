package com.wu.wuaicode.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ClassLoadingTest {
    
    @Test
    public void testWhichClassIsLoaded() {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("dev.langchain4j.model.openai.OpenAiStreamingChatModel");
            System.out.println("Loaded from: " + clazz.getProtectionDomain().getCodeSource().getLocation());
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found");
        }
    }
}