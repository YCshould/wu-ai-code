package com.wu.wuaicode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.wu.wuaicode.ai.model.HtmlCodeResult;
import com.wu.wuaicode.ai.model.MultiFileCodeResult;
import com.wu.wuaicode.exception.BusinessException;
import com.wu.wuaicode.exception.ErrorCode;
import com.wu.wuaicode.model.enums.CodeGenTypeEnum;

public class MultiFileCodeFileSaverTemplate extends CodeFileSaveTemplate<MultiFileCodeResult>{

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFile(MultiFileCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

    @Override
    protected void validateContent(MultiFileCodeResult result) {
        super.validateContent(result);
        // HTML 代码不能为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}
