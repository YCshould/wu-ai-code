package com.wu.wuaicode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wu.wuaicode.ai.AiCodeGenTypeRoutingService;
import com.wu.wuaicode.ai.AiCodeGenTypeRoutingServiceFactory;
import com.wu.wuaicode.constant.AppConstant;
import com.wu.wuaicode.core.AiCodeGeneratorFacade;
import com.wu.wuaicode.core.builder.VueProjectBuilder;
import com.wu.wuaicode.core.handler.StreamHandlerExecutor;
import com.wu.wuaicode.exception.BusinessException;
import com.wu.wuaicode.exception.ErrorCode;
import com.wu.wuaicode.exception.ThrowUtils;
import com.wu.wuaicode.model.dto.app.AppAddRequest;
import com.wu.wuaicode.model.dto.app.AppQueryRequest;
import com.wu.wuaicode.model.entity.App;
import com.wu.wuaicode.mapper.AppMapper;
import com.wu.wuaicode.model.entity.User;
import com.wu.wuaicode.model.enums.ChatHistoryMessageTypeEnum;
import com.wu.wuaicode.model.enums.CodeGenTypeEnum;
import com.wu.wuaicode.model.vo.app.AppVO;
import com.wu.wuaicode.model.vo.user.UserVO;
import com.wu.wuaicode.service.AppService;
import com.wu.wuaicode.service.ChatHistoryService;
import com.wu.wuaicode.service.ScreenshotService;
import com.wu.wuaicode.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author Author: Xuehai Wu
 */
@Slf4j
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory  aiCodeGenTypeRoutingServiceFactory;

    /**
     * 只是创建应用
     * @param appAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        log.info(initPrompt);
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        app.setInitPrompt(initPrompt);
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型
//        AiCodeGenTypeRoutingService routingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
//        CodeGenTypeEnum selectedCodeGenType = routingService.routeCodeGenType(initPrompt);
        CodeGenTypeEnum codeGenTypeEnum = appAddRequest.getCodeGenTypeEnum();
        app.setCodeGenType(codeGenTypeEnum.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), codeGenTypeEnum.getValue());
        return app.getId();
    }



    /**
     * 部署应用
     * @param appId
     * @param loginUser
     * @return
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 将 dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        }

        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 9. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 生成部署后可访问的 URL
        String deployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 11、异步截图，并将截图后的文件上传到cos中
        screenshotAndUpdate(appId,deployUrl);
        return deployUrl;
    }


    private void screenshotAndUpdate(long appId,String deployUrl){
        Thread.startVirtualThread(()->{
            // 1、完成截图并保存到cos,返回图片的访问路径
            String cosUrl = screenshotService.screenshotUpdate(deployUrl);
            // 2、保存图片的访问路径
            App app=new App();
            app.setId(appId);
            app.setCover(cosUrl);
            boolean b = this.updateById(app);
            if (!b) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"更新应用封面到数据库失败");
            }
        });
    }

    /**
     * 生成用户代码
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
    @Override
    public Flux<String> chatGneCode(Long appId, String message, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(appId==null||appId<0,ErrorCode.PARAMS_ERROR,"appId格式错误");
        if(StrUtil.isBlank(message)){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR,"message不能为空");
        }
        //获取应用的枚举类型
        App app = this.getById(appId);
        if(app==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"应用不存在");
        }
        String codeGenType = app.getCodeGenType();
        Long userId = app.getUserId();
        if(!userId.equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"非法操作,无权访问该应用");
        }
        CodeGenTypeEnum enumByValue = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if(enumByValue==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"应用的codeGenType格式错误");
        }
        //  通过校验后，添加用户消息到对话历史
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        //  调用 AI 生成代码（流式）
        Flux<String> contentFlux = aiCodeGeneratorFacade.generateCodeAndSaveStream(message, enumByValue, appId);
        //  收集AI响应内容并在完成后记录到对话历史
        StringBuilder aiResponseBuilder = new StringBuilder();
//        return contentFlux
//                .map(chunk -> {
//                    // 收集AI响应内容
//                    aiResponseBuilder.append(chunk);
//                    return chunk;
//                })
//                .doOnComplete(() -> {
//                    // 流式响应完成后，添加AI消息到对话历史
//                    String aiResponse = aiResponseBuilder.toString();
//                    if (StrUtil.isNotBlank(aiResponse)) {
//                        chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
//                    }
//                })
//                .doOnError(error -> {
//                    // 如果AI回复失败，也要记录错误消息
//                    String errorMessage = "AI回复失败: " + error.getMessage();
//                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
//                });
        return streamHandlerExecutor.doExecute(contentFlux,chatHistoryService,appId,loginUser,enumByValue);
    }

    /**
     * 重写删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }


    /**
     * 获取应用VO对象
     * @param app
     * @return
     */
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }


    /**
     * 构造查询条件
     * @param appQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 分页查询应用列表批量获取应用VO对象
     * @param appList
     * @return
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }




}
