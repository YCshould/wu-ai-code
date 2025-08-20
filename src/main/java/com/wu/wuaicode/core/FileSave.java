package com.wu.wuaicode.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.wu.wuaicode.ai.model.HtmlCodeResult;
import com.wu.wuaicode.ai.model.MultiFileCodeResult;
import com.wu.wuaicode.model.enums.CodeGenTypeEnum;

import java.io.File;
import static cn.hutool.core.io.FileUtil.mkdir;

@Deprecated
public class FileSave {
    /**
     * 保存代码到文件
     * 先根据创建的不同文件路径创建文件夹再将内容到指定文件中
     */

    //定义文件路径
    private static final String FILE_PATH = System.getProperty("user.dir")+File.separator+"tmp"+File.separator+"outCode";

    //保存html代码到文件
    public static File saveHtml(HtmlCodeResult htmlCodeResult) {
        String baseDirPath = filePath(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDirPath, "index.html", htmlCodeResult.getHtmlCode());
        return new File(baseDirPath);
    }

    //保存多文件代码到文件
    /**
     * 保存 MultiFileCodeResult
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult result) {
        String baseDirPath = filePath(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
        return new File(baseDirPath);
    }

    //定义文件路径
    private static String filePath(String bizType){
        String uniquefilename = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextId());
        String filepath=FILE_PATH+File.separator+uniquefilename;
        mkdir(filepath);
        return filepath;
    }

    //保存单个文件代码到文件
    private static File writeToFile(String path, String fileName, String content) {
        String filepath=path+File.separator+fileName;
        return FileUtil.writeString(content, filepath, CharsetUtil.UTF_8);  //writeUtf8String严格遵循传参规定，第一个参数为文件内容，第二个参数为文件路径
                                                                            //因为writeUtf8String还是调用的是writeString
    }

}

