package com.wu.wuaicode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.wu.wuaicode.model.dto.app.AppQueryRequest;
import com.wu.wuaicode.model.entity.App;
import com.wu.wuaicode.model.vo.app.AppVO;

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

}
