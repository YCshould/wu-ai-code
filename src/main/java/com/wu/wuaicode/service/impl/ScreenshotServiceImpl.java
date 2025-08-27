package com.wu.wuaicode.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.wu.wuaicode.exception.BusinessException;
import com.wu.wuaicode.exception.ErrorCode;
import com.wu.wuaicode.manager.CosManager;
import com.wu.wuaicode.service.ScreenshotService;
import com.wu.wuaicode.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ScreenshotServiceImpl implements ScreenshotService {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotServiceImpl.class);
    @Resource
    private CosManager cosManager;

    /**
     * 截图并将截图文件上传到cos
     * 最后一定要清理本地文件
     * @param webUrl
     * @return
     */
    @Override
    public String screenshotUpdate(String webUrl) {
        // 1、校验参数
        if(webUrl==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"网页地址不能为空");
        }
        // 2、截图
        String screenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        if(StrUtil.isBlank(screenshotPath)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"截图失败");
        }
        // 3、上传截图文件到cos
        try {
            String screenshotUrl = updateScreenshotToCos(screenshotPath);
            if(StrUtil.isBlank(screenshotUrl)){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"上传本地截图到cos失败");
            }
            log.info("上传本地截图到cos成功{}->{}",webUrl,screenshotUrl);
            return screenshotUrl;
        } finally {
            // 4、清除本地截图文件
            clearScreenshot(screenshotPath);
        }
    }

    /**
     *
     * @param screenshotPath
     * @return
     */
    private String updateScreenshotToCos(String screenshotPath) {
        if(StrUtil.isBlank(screenshotPath)) {
            return null;
        }
        File file = new File(screenshotPath);
        if(!file.exists()){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"截图文件不存在");
        }
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.jpg";
        String generatorKey = generatorKey(fileName);
        return cosManager.uploadFile(generatorKey, file);
    }

    /**
     * 生成文件cos保存key
     * @param fileName
     * @return
     */
    private String generatorKey(String fileName) {
        String datePath= LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("/screenshot/%s%s",datePath,fileName);
    }

    /**
     * 清除本地截图文件
     * @param screenshotLocalPath
     * @return
     */
    private void clearScreenshot(String screenshotLocalPath) {
        File file = new File(screenshotLocalPath);
        if(file.exists()){
            File parentFile = file.getParentFile();
            FileUtil.del(parentFile);
            log.info("本地文件清除成功");
        }
    }
}
