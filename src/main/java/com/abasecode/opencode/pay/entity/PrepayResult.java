package com.abasecode.opencode.pay.entity;

import com.abasecode.opencode.pay.plugin.wechatpay.entity.MicroParam;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author Jon
 * e-mail: ijonso123@gmail.com
 * url: <a href="https://jon.wiki">Jon's blog</a>
 * url: <a href="https://github.com/abasecode">project github</a>
 * url: <a href="https://abasecode.com">AbaseCode.com</a>
 */
@Data
@Accessors(chain = true)
public class PrepayResult implements Serializable {

    private static final long serialVersionUID = -7401634860330043604L;
    /**
     * 支付宝和微信公众号扫码的url
     */
    private String url;
    /**
     * 预支付的中文信息
     */
    private String msg;
    /**
     * 微信小程序独有
     */
    private MicroParam param;
}
