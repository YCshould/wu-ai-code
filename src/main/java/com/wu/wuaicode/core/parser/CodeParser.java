package com.wu.wuaicode.core.parser;

/**
 * 定义代码解析器接口
 * 后续的parseHtmlCode和parseMultiFileCode方法将实现该接口
 */
public interface CodeParser<T> {

    T parseCode(String codeContent);
}
