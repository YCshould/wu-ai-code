package com.wu.wuaicode.controller;

import com.wu.wuaicode.common.BaseResponse;
import com.wu.wuaicode.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/ok")
    public BaseResponse<String> codeCheck() {
        return ResultUtils.success("ok");
    }
}

