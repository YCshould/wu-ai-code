package com.wu.wuaicode.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.wu.wuaicode.exception.BusinessException;
import com.wu.wuaicode.exception.ErrorCode;
import com.wu.wuaicode.model.enums.CodeGenTypeEnum;
import java.io.File;
import static cn.hutool.core.io.FileUtil.mkdir;

/**
 * 这是保存代码文件的公共模板
 * 在这个公共模板上实现保存html和multi代码文件的逻辑，也以模板实现，再通过执行器来实现按需调用
 * 只要没加final修饰的方法都可以，子类都可以去重写
 */
public abstract class CodeFileSaveTemplate<T> {

    protected static final String FILE_PATH = System.getProperty("user.dir")+ File.separator+"tmp"+File.separator+"outCode";

    /**
     * 最终外部还是调用这个方法
     * @param content
     * @return
     */
    public final File saveCode(T content) {
        //验证输入内容合法性
        validateContent(content);
        //定义文件路径,可交给子类去实现
        String filedPath = filePath();
        //保存代码文件（有可能多个代码文件）
        saveFile(content, filedPath);
        return new File(filedPath);
    }

    /**
     * 验证输入内容合法性
     * 交给子类模板去实现
     *
     * @param content
     * @return
     */
    protected void validateContent(T content) {
        if(content==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"保存代码文件失败，输入内容为空");
        }
    }

    /**
     * 定义文件路径
     */
    //定义文件路径
    protected final String filePath(){
        String bizType = getCodeType().getValue();
        String uniquefilename = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextId());
        String filepath=FILE_PATH+File.separator+uniquefilename;
        mkdir(filepath);
        return filepath;
    }




    //保存单个文件代码到文件的工具
    protected final void writeToFile(String path, String fileName, String content) {
        if(content!=null){
            String filepath=path+File.separator+fileName;
            //writeUtf8String严格遵循传参规定，第一个参数为文件内容，第二个参数为文件路径
            //因为writeUtf8String还是调用的是writeString
            FileUtil.writeString(content, filepath, CharsetUtil.UTF_8);
        }
    }


    /**
     * 保存单个代码文件逻辑给子类实现
     * @param filePath
     * @param content
     */
    protected abstract void saveFile(T content,String filePath);

    /**
     * 子类实现获取文件类型
     * @return
     */
    protected abstract CodeGenTypeEnum getCodeType();
}
