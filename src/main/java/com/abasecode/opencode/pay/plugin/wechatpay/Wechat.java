package com.abasecode.opencode.pay.plugin.wechatpay;

import com.abasecode.opencode.pay.constant.PayConstant;
import com.abasecode.opencode.pay.entity.BaseOrder;
import com.abasecode.opencode.pay.entity.PayType;
import com.abasecode.opencode.pay.plugin.wechatpay.constant.WechatConstant;
import com.abasecode.opencode.pay.plugin.wechatpay.entity.*;
import com.abasecode.opencode.pay.plugin.wechatpay.util.WechatHttp;
import com.abasecode.opencode.pay.util.BaseUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author Jon
 * url: <a href="https://jon.wiki">Jon's blog</a>
 */
@Component
@Slf4j
public class Wechat {

    /**
     * 付款流程：公众号支付第一步（仅两步）
     * 生成公众号JSAPI获取code的URL，通过微信浏览器打开URL，等待微信回调返回code
     *
     * @param state 可不传
     * @return url
     * @throws UnsupportedEncodingException
     */
    public String jsapiMpPayStep1(String state) throws UnsupportedEncodingException {
        if (StringUtils.isEmpty(state)) {
            state = "STATE";
        }
        String url = URLEncoder.encode(
                WechatConstant.wechatCodeReturnUrl,
                StandardCharsets.UTF_8.toString()
        );
        String fullUrl = WechatConstant.URL_JSAPI_CODE.replace("{APPID}", WechatConstant.wechatMpAppid)
                .replace("{REDIRECT_URI}", url)
                .replace("{SCOPE}", "snsapi_base")
                .replace("{STATE}", state);
        return fullUrl;
    }

    /**
     * 付款流程：公众号第二步（仅两步）
     * 1、使用code换accessToken（内含openId）
     * 2、使用openId封装payer
     * 3、使用payer封装支付参数，获得Pay4Jsapi
     * 4、向微信服务器发送支付参数，获得prepay_id
     * 5、使用prepay_id封装前端参数，发送给前端
     * 6、用户通过前端按钮触发将参数发送给微信服务器，支付完成
     *
     * @param payType 支付类型
     * @param order   订单
     * @param code    微信返回的code
     * @return ClientPayParam
     * @throws Exception
     */
    public WechatClientPayParam jsapiMpPayStep2(PayType payType, BaseOrder order, String code) throws Exception {
        String appId = getAppId(payType);
        AccessToken accessToken = getAccessToken(code);
        Payer payer = new Payer().setOpenid(accessToken.getOpenid());
        Pay4Jsapi pay4Jsapi = createJsapiOrderParams(payType, order, payer);
        String s = JSON.toJSONString(pay4Jsapi);
        return getWechatClientPayParam(appId, s);
    }

    /**
     * 付款流程，第二步的第1小步
     * 通过code获取AccessToken
     *
     * @param code 第一步获取的code
     * @return AccessToken
     */
    private AccessToken getAccessToken(String code) throws Exception {
        String accessUrl = WechatConstant.URL_JSAPI_ACCESS_TOKEN.replace("{APPID}", WechatConstant.wechatMpAppid)
                .replace("{SECRET}", WechatConstant.wechatMpSecret)
                .replace("{CODE}", code);
        JSONObject object = WechatHttp.httpGet(accessUrl, "", JSONObject.class);
        AccessToken accessToken = new AccessToken();
        accessToken.setAccessToken(object.getString("access_token"))
                .setExpiresIn(object.getLong("expires_in"))
                .setRefreshToken(object.getString("refresh_token"))
                .setOpenid(object.getString("openid"))
                .setScope(object.getString("scope"))
                .setIsSnapshotuser(object.getInteger("is_snapshotuser"))
                .setErrcode(object.getString("errcode"))
                .setErrmsg(object.getString("errmsg"));
        return accessToken;
    }

    /**
     * 根据微信支付类型返回AppID
     *
     * @param payType
     * @return appid
     */
    private String getAppId(PayType payType) {
        switch (payType) {
            case WECHAT_APP:
                return WechatConstant.wechatAppAppid;
            case WECHAT_JSAPI_MICRO:
                return WechatConstant.wechatMicroAppid;
            default:
                return WechatConstant.wechatMpAppid;
        }
    }

    /**
     * 封装JSAPI 订单参数（小程序，公众号适用）
     * 公众号：付款流程，第二步的第3小步
     * 小程序：付款流程，第二步
     * 用于发送给微信服务器，以便获得prepay_id
     * @param payType
     * @param baseOrder
     * @param payer
     * @return
     */
    private Pay4Jsapi createJsapiOrderParams(PayType payType, BaseOrder baseOrder, Payer payer) {
        AmountOrder amount = new AmountOrder()
                .setCurrency(WechatConstant.CURRENCY)
                .setTotal(baseOrder.getAmount());
        Pay4Jsapi order = new Pay4Jsapi()
                .setAppid(getAppId(payType))
                .setMchid(WechatConstant.wechatMchid)
                .setOutTradeNo(baseOrder.getOutTradeNo())
                .setDescription(baseOrder.getSubject())
                .setNotifyUrl(WechatConstant.wechatPayNotifyUrl)
                .setAmount(amount)
                .setPayer(payer);
        if (StringUtils.isNotEmpty(baseOrder.getOtherParams())) {
            order.setAttach(baseOrder.getOtherParams());
        }
        if (StringUtils.isNotEmpty(baseOrder.getTimeExpire())) {
            order.setTimeExpire(baseOrder.getTimeExpire());
        }
        return order;
    }


//    /**
//     * 小程序支付流程，第一步，使用code获得openId
//     * @param code 小程序前端返回的code
//     * @return Code2SessionResult
//     * @throws Exception
//     */
//    public Code2SessionResult jsapiMicroPayStep1(String code) throws Exception {
//        String url = WechatConstant.URL_CODE2SESSION.replace("{APPID}", WechatConstant.wechatMicroAppid)
//                .replace("{SECRET}", WechatConstant.wechatMicroSecret)
//                .replace("{JSCODE}", code);
//
//        Code2SessionResult result = WechatHttp.httpGet(url, "", Code2SessionResult.class);
//        return result;
//    }

    /**
     * 付款流程：小程序第1步（仅1步）
     * @param order 订单信息
     * @param code 前端的code
     * @return 小程序前端需要的参数（WechatClientPayParam）
     * @throws Exception
     */
    public WechatClientPayParam jsapiMicroPay(BaseOrder order,String code) throws Exception {
        if (null == order) {
            throw new Exception(PayConstant.MSG_NOT_NULL_ORDER);
        }
        Code2SessionResult result = microPayGetOpenId(code);
        Payer payer = new Payer().setOpenid(result.getOpenid());

        String appId = getAppId(PayType.WECHAT_JSAPI_MICRO);
        Pay4Jsapi pay4Jsapi = createJsapiOrderParams(PayType.WECHAT_JSAPI_MICRO, order, payer);
        String s = JSON.toJSONString(pay4Jsapi);
        return getWechatClientPayParam(appId, s);
    }

    /**
     * 向微信服务器发起请求获得prepay_id
     * @param appId appid
     * @param json 要传给微信服务器的参数json
     * @return 小程序前端需要的参数（WechatClientPayParam）
     * @throws Exception
     */
    private WechatClientPayParam getWechatClientPayParam(String appId, String json) throws Exception {
        Pay4JsapiReturn r = WechatHttp.httpPost(WechatConstant.URL_JSAPI_ORDER, json, Pay4JsapiReturn.class);
        WechatClientPayParam param = new WechatClientPayParam()
                .setAppid(appId)
                .setPackages("prepay_id=" + r.getPrepayId())
                .setTimeStamp(BaseUtils.getCurrentTimeStamp())
                .setNonceStr(BaseUtils.getNonceStr())
                .setSignType(WechatConstant.SIGN_TYPE);
        String jsapiSign = WechatHttp.getJsapiSign(appId, param.getTimeStamp(), param.getNonceStr(), param.getPackages());
        param.setPaySign(jsapiSign);
        return param;
    }

    /**
     * 小程序获得 openId
     * @param code 传入前端的code
     * @return Code2SessionResult（含openId）
     * @throws Exception
     */
    private Code2SessionResult microPayGetOpenId(String code) throws Exception {
        String url = WechatConstant.URL_CODE2SESSION.replace("{APPID}", WechatConstant.wechatMicroAppid)
                .replace("{SECRET}", WechatConstant.wechatMicroSecret)
                .replace("{JSCODE}", code);
        Code2SessionResult r = WechatHttp.httpGet(url, "", Code2SessionResult.class);
        return r;
    }



}
