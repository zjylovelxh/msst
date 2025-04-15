package com.zjy.mianshist.sa_token;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.zjy.mianshist.common.ErrorCode;
import com.zjy.mianshist.exception.ThrowUtils;



import javax.servlet.http.HttpServletRequest;


/**
 * 判断用户登录是哪一种设备   小程序，平板，手机，平板
 */
public class identifyDivice {

    public  static String getDevice(HttpServletRequest request){

        String header = request.getHeader(Header.USER_AGENT.toString());
        UserAgent userAgent = UserAgentUtil.parse(header);
        ThrowUtils.throwIf(userAgent == null, ErrorCode.PARAMS_ERROR,"非法请求！");
        String device = "pc";
        if(StrUtil.containsIgnoreCase(header,"MicroMessenger") && StrUtil.containsIgnoreCase(header,"MiniProgram")){
            device = "miniProgram";
        } else if (StrUtil.containsIgnoreCase(header,"iPad") || (StrUtil.containsIgnoreCase(header,"Android") && !StrUtil.containsIgnoreCase(header,"Mobile"))) {
            device="pad";
        }else if (StrUtil.containsIgnoreCase(header,"Mobile")) {
            device="mobile";
        }
        return device;
    }
}
