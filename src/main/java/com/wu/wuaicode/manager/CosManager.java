package com.wu.wuaicode.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.wu.wuaicode.config.CosClientConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传截图文件到cos中，拼接一个图片的访问url
     * @param key
     * @param file  截图文件
     * @return
     */
    public String uploadFile(String key, File file) {
        // 1、先上传对象
        PutObjectResult putObjectResult = putObject(key, file);
        if(putObjectResult != null) {
            String url=String.format("%s%s", cosClientConfig.getHost(), key);
            log.info("截图文件上传到cos中成功,文件链接为：{}",url);
            return url;
        }else {
            log.error("文件上传失败");
            return null;
        }
    }
}
