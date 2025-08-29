package com.wu.wuaicode.model.dto.app;

import com.wu.wuaicode.model.enums.CodeGenTypeEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 代码生成类型
     */
    private CodeGenTypeEnum codeGenTypeEnum;

    private static final long serialVersionUID = 1L;
}
