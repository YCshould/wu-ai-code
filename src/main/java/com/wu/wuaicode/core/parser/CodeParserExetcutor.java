package com.wu.wuaicode.core.parser;

import com.wu.wuaicode.exception.BusinessException;
import com.wu.wuaicode.exception.ErrorCode;
import com.wu.wuaicode.model.enums.CodeGenTypeEnum;

/**
 * 最终就是调用这个，返回不同的解析器，并解析出
 * 也就是策略模式
 */
public class CodeParserExetcutor {

    private static final  ParseHtmlCode parseHtmlCode=new ParseHtmlCode();

    private static final ParseMultiFileCode parseMultiFileCode=new ParseMultiFileCode();

    public static Object parserExecutor( String codeContent,CodeGenTypeEnum codeGenTypeEnum){
        return switch (codeGenTypeEnum){
            case HTML -> parseHtmlCode.parseCode(codeContent);
            case MULTI_FILE -> parseMultiFileCode.parseCode(codeContent);
            default -> {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码类型"+codeGenTypeEnum.getValue());
            }
        };

    }
}
