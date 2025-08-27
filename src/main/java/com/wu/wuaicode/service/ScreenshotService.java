package com.wu.wuaicode.service;

public interface ScreenshotService {

    /**
     *截图并将截图文件上传到cos
     */
    String screenshotUpdate(String webUrl);
}
