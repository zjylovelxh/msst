package com.zjy.mianshist.sa_token;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.zjy.mianshist.model.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.zjy.mianshist.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 获取权限集合方便注解使用
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {


    @Override
    public List<String> getPermissionList(Object o, String s) {
        return List.of(); //返回空集合
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = (User)StpUtil.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);  // 获取当前登录用户
        return Collections.singletonList(user.getUserRole());// 返回一个账号所拥有的角色标识集合
    }

}

