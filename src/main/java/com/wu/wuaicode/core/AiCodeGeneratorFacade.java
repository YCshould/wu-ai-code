package com.wu.wuaicode.core;

import com.wu.wuaicode.ai.AiCodeGeneratorService;
import com.wu.wuaicode.ai.model.HtmlCodeResult;
import com.wu.wuaicode.ai.model.MultiFileCodeResult;
import com.wu.wuaicode.core.parser.CodeParserExetcutor;
import com.wu.wuaicode.core.saver.CodeFileSaveExecutor;
import com.wu.wuaicode.exception.BusinessException;
import com.wu.wuaicode.exception.ErrorCode;
import com.wu.wuaicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * 门面模式，对外提供服务
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 后续只调用这一个方法即可实现调用ai并保存文件
     * 普通ai模型
     * @param userMessage
     * @param codeGenType
     * @return
     */
    public File generateCodeAndSave(String userMessage, CodeGenTypeEnum codeGenType){

        if(codeGenType==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"未知的代码生成类型");
        }
        return switch (codeGenType){
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield  CodeFileSaveExecutor.codeFileSaveExecutor(htmlCodeResult,CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultFileCode(userMessage);
                yield  CodeFileSaveExecutor.codeFileSaveExecutor(multiFileCodeResult,CodeGenTypeEnum.MULTI_FILE);
            }
            default -> {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"未知的代码生成类型");
            }
        };
    }

//    private File generateAndSaveHtml(String userMessage) {
//        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
//        return FileSave.saveHtml(htmlCodeResult);
//    }
//
//    private File generateAndSaveMultiFile(String userMessage) {
//        MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultFileCode(userMessage);
//        return FileSave.saveMultiFileCodeResult(multiFileCodeResult);
//    }

    /**
     * 流式调用，返回Flux<String>，可以对其进行流式处理
     * @param userMessage
     * @param codeGenType
     * @return
     */
    public Flux<String> generateCodeAndSaveStream(String userMessage, CodeGenTypeEnum codeGenType){

        if(codeGenType==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"未知的代码生成类型");
        }
        return switch (codeGenType){
            case HTML -> {
                Flux<String> htmlStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield  generateAndSaveStream(htmlStream, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                Flux<String> multFileStream = aiCodeGeneratorService.generateMultFileCodeStream(userMessage);
                yield  generateAndSaveStream(multFileStream, CodeGenTypeEnum.MULTI_FILE);
            }
            default -> {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"未知的代码生成类型");
            }
        };
    }

    private Flux<String> generateAndSaveStream(Flux<String> userMessageStream,CodeGenTypeEnum codeGenType) {

        StringBuilder stringBuilder = new StringBuilder();
        return userMessageStream.doOnNext(chunk->{
            //每拿到一块代码块就要用字符拼接器对其进行拼接
            stringBuilder.append(chunk);
        }).doOnComplete(()->{
            try {
                //当所有代码块都拿到后，用代码解析器把一串的代码字符解析为结构化的HTML代码
                String codeParing = stringBuilder.toString();
                Object parserExecutor = CodeParserExetcutor.parserExecutor(codeParing, codeGenType);
                File file = CodeFileSaveExecutor.codeFileSaveExecutor(parserExecutor, codeGenType);
                log.info("MultiFileCode文件保存成功,路径为:{}",file.getAbsolutePath());
            } catch (Exception e) {
                log.error("解析MultiFileCode代码出错,保存MultiFileCode文件失败");
            }
        });
//        return userMessageStream;
//        这意味着调用方将接收到的是未经过处理的原始流
//        您添加的doOnNext和doOnComplete操作实际上只在方法内部创建了新的处理链
//        但没有被实际订阅或返回，因此永远不会执行
//        所以不能这样写
    }

//    private Flux<String> generateAndSaveHtmlStream(String userMessage) {
//        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
//        StringBuilder stringBuilder = new StringBuilder();
//        result.doOnNext(chunk->{
//            //每拿到一块代码块就要用字符拼接器对其进行拼接
//            stringBuilder.append(chunk);
//        }).doOnComplete(()->{
//            try {
//                //当所有代码块都拿到后，用代码解析器把一串的代码字符解析为结构化的HTML代码
//                String codeParing = stringBuilder.toString();
//                HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(codeParing);
//                File file = FileSave.saveHtml(htmlCodeResult);
//                log.info("HTML文件保存成功,路径为:{}",file.getAbsolutePath());
//            } catch (Exception e) {
//                log.error("解析HTML代码出错,保存HTML文件失败");
//            }
//        });
//        return result;
//    }

}



