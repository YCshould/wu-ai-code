package com.wu.wuaicode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.wu.wuaicode.model.dto.app.AppAddRequest;
import com.wu.wuaicode.model.dto.app.AppQueryRequest;
import com.wu.wuaicode.model.entity.App;
import com.wu.wuaicode.model.entity.User;
import com.wu.wuaicode.model.vo.app.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author Author: Xuehai Wu
 */
public interface AppService extends IService<App> {

    /**
     * 获取应用脱敏后的信息
     * @param app
     * @return
     */
     AppVO getAppVO(App app);

    /**
     * 获取查询条件的QueryWrapper
     * @param appQueryRequest
     * @return
     */
     QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 分页获取应用列表的VO对象
     * @param appList
     * @return
     */
     List<AppVO> getAppVOList(List<App> appList);

    /**
     * 生成应用代码服务
     * @return
     */
    Flux<String> chatGneCode(Long appId, String message, User loginUser);

    /**
     * 只是创建应用
     * @param appAddRequest
     * @param loginUser
     * @return
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 部署应用
     * @param appId
     * @param loginUser
     * @return
     */
    String deployApp(Long appId, User loginUser);

}
