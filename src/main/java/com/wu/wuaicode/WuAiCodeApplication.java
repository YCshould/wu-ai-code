package com.wu.wuaicode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wu.wuaicode.mapper")
public class WuAiCodeApplication {

	public static void main(String[] args) {
		SpringApplication.run(WuAiCodeApplication.class, args);
	}

}
