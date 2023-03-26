package com.abasecode.opencode.pay.plugin.wechatpay.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author Jon
 * url: <a href="https://jon.wiki">Jon's blog</a>
 * <p>
 * 小程序通过code换取openid
 * 登录凭证校验。通过 wx.login 接口获得临时登录凭证 code 后传到开发者服务器调用此接口完成登录流程。更多使用方法详见小程序登录。
 * https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html
 */
@Data
@Accessors(chain = true)
public class Code2SessionResult implements Serializable {

    private static final long serialVersionUID = -8747028933689239502L;
    @JSONField(name = "session_key")
    private String sessionKey;
    @JSONField(name = "unionid")
    private String unionId;
    @JSONField(name = "errmsg")
    private String msg;
    private String openid;
    @JSONField(name = "errcode")
    private String errCode;
}
