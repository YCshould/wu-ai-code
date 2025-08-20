package com.wu.wuaicode.core.saver;


import com.wu.wuaicode.ai.model.HtmlCodeResult;
import com.wu.wuaicode.ai.model.MultiFileCodeResult;
import com.wu.wuaicode.exception.BusinessException;
import com.wu.wuaicode.exception.ErrorCode;
import com.wu.wuaicode.model.enums.CodeGenTypeEnum;

import java.io.File;

public class CodeFileSaveExecutor {

    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaverTemplate=new HtmlCodeFileSaverTemplate();

    private static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaverTemplate=new MultiFileCodeFileSaverTemplate();

    public static File codeFileSaveExecutor(Object codeContent, CodeGenTypeEnum codeGenTypeEnum){
        return switch (codeGenTypeEnum){
            case HTML -> htmlCodeFileSaverTemplate.saveCode((HtmlCodeResult) codeContent);
            case MULTI_FILE -> multiFileCodeFileSaverTemplate.saveCode((MultiFileCodeResult) codeContent);
            default -> {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码类型"+codeGenTypeEnum.getValue());
            }
        };

    }
}
