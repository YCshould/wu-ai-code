package com.wu.wuaicode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.wu.wuaicode.ai.model.HtmlCodeResult;
import com.wu.wuaicode.exception.BusinessException;
import com.wu.wuaicode.exception.ErrorCode;
import com.wu.wuaicode.model.enums.CodeGenTypeEnum;

/**
 * HTML代码文件保存器
 *
 * @author yupi
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaveTemplate<HtmlCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFile(HtmlCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    @Override
    protected void validateContent(HtmlCodeResult result) {
        super.validateContent(result);
        // HTML 代码不能为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}
