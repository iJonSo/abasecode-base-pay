package com.abasecode.opencode.pay;

import com.abasecode.opencode.pay.constant.PayConstant;
import com.abasecode.opencode.pay.entity.*;
import com.abasecode.opencode.pay.form.PayCloseForm;
import com.abasecode.opencode.pay.form.PayQueryForm;
import com.abasecode.opencode.pay.form.PayRefundForm;
import com.abasecode.opencode.pay.form.PayRefundQueryForm;
import com.abasecode.opencode.pay.plugin.alipay.AlipayHandler;
import com.abasecode.opencode.pay.plugin.wechatpay.WechatHandler;
import com.abasecode.opencode.pay.plugin.wechatpay.constant.WechatConstant;
import com.abasecode.opencode.pay.plugin.wechatpay.entity.*;
import com.abasecode.opencode.pay.util.BaseUtils;
import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.response.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Objects;

import static com.abasecode.opencode.pay.util.BaseUtils.*;

/**
 * @author Jon
 * e-mail: ijonso123@gmail.com
 * url: <a href="https://jon.wiki">Jon's blog</a>
 * url: <a href="https://github.com/abasecode">project github</a>
 * url: <a href="https://abasecode.com">AbaseCode.com</a>
 */
@Component
public class BasePay {

    @Autowired
    private WechatHandler wechatHandler;
    @Autowired
    private AlipayHandler alipayHandler;


    /**
     * 聚合支付：发起预支付
     *
     * @param payChannel 支付通道
     * @param payType    支付类型
     * @param order      订单
     * @param code       微信小程序前端返回的code
     * @return 预支付信息：支付宝返回前端可调用支付表单，微信返回前端调用的url
     * @throws AlipayApiException
     * @throws UnsupportedEncodingException
     */
    public PrepayResult prepay(PayChannel payChannel, PayType payType, BaseOrder order, String code) throws Exception {
        checkPayChannelAndType(payChannel, payType);
        if (null == order) {
            throw new Exception(PayConstant.MSG_NOT_NULL_ORDER);
        }
        PrepayResult result = new PrepayResult();
        if (payChannel == PayChannel.ALIPAY) {
            AlipayTradeWapPayResponse response = alipayHandler.prepayWap(payType, order);
            result.setUrl(response.getBody());
            result.setMsg("OK");
            return result;
        }
        if (payChannel == PayChannel.WECHAT) {
            String url;
            if (payType == PayType.WECHAT_JSAPI_MP) {
                url = wechatHandler.createJsapiCodeUrl(order.getOutTradeNo());
                result.setUrl(url);
                result.setMsg("OK");
                return result;
            }
            if (payType == PayType.WECHAT_JSAPI_MICRO) {

                Code2SessionResult r = wechatHandler.microPayGetOpenId(code);
                WechatClientPayParam payParam = wechatHandler.prePayJsapiMicro(PayType.WECHAT_JSAPI_MICRO, order, r.getOpenid());
                if (payParam != null) {
                    MicroParam p = new MicroParam()
                            .setPaySign(payParam.getPaySign())
                            .setPackages(payParam.getPackages())
                            .setTimeStamp(payParam.getTimeStamp())
                            .setNonceStr(payParam.getNonceStr())
                            .setSignType(payParam.getSignType());
                    result.setMsg("OK");
                    result.setParam(p);
                    return result;
                }
                result.setMsg("ERROR");
                result.setUrl("null");
            }
        }
        result.setMsg(PayConstant.MSG_PAY_CHANNEL_SUPPORT);
        return result;
    }


//    /**
//     * 聚合支付：发起预支付(小程序)
//     *
//     * @param order      订单
//     * @param code 小程序前端登录获取的code
//     * @return
//     * @throws Exception
//     */
//    public WechatClientPayParam prepayMicro(BaseOrder order,String code) throws Exception {
//        // 参考地址 https://juejin.cn/post/7079049378983313438
//        // 1. 使用code 获得 openId
//        // 2. 调用统一下单，生成prepay_id （prePayJsapiMicro 调用 createClientPayParam）
//        // 3. 组装并返回给小程序前端 (WechatClientPayParam)
//        // 4. 用户在小程序支付
//        // 5. 回调
//        // 根据以上顺序，做个demo
//
////        checkPayChannelAndType(payChannel, payType);
//        if (null == order) {
//            throw new Exception(PayConstant.MSG_NOT_NULL_ORDER);
//        }
//        Code2SessionResult result = wechatHandler.microPayGetOpenId(code);
//        WechatClientPayParam payParam = wechatHandler.prePayJsapiMicro(PayType.WECHAT_JSAPI_MICRO, order, result.getOpenid());
//        return payParam;
//
////        PrepayResult result = new PrepayResult();
//
////        url = wechatHandler.createJsapiOpenIdUrl(code);
////        result.setPrePayResult(url);
////        return result;
//
////        result.setPrePayResult(PayConstant.MSG_PAY_CHANNEL_SUPPORT);
////        return result;
//    }


//    /**
//     * 聚合支付：获得预支付参数（仅微信）
//     *
//     * @param payChannel   通道
//     * @param payType      支付类型
//     * @param order        订单
//     * @param codeOrOpenId code或openId（code是公众号，openId是小程序）
//     * @return
//     */
//    public WechatClientPayParam prepayStep2(PayChannel payChannel, PayType payType, BaseOrder order, String codeOrOpenId) throws Exception {
//        checkPayChannelAndType(payChannel, payType);
//        if (null == order) {
//            throw new Exception(PayConstant.MSG_NOT_NULL_ORDER);
//        }
//        if (StringUtils.isBlank(codeOrOpenId)) {
//            throw new Exception(PayConstant.MSG_NOT_NULL_PAY_CODE_OPENID);
//        }
//        if (payChannel == PayChannel.WECHAT) {
//            if (payType == PayType.WECHAT_JSAPI_MP) {
//                return wechatHandler.prePayJsapiMp(payType, order, codeOrOpenId);
//            }
//            if (payType == PayType.WECHAT_JSAPI_MICRO) {
//                return wechatHandler.prePayJsapiMicro(payType, order, codeOrOpenId);
//            }
//        }
//        throw new Exception(PayConstant.MSG_PAY_CHANNEL_ONLY_WECHAT);
//    }

    /**
     * 查询订单
     *
     * @param form 查询
     * @return 查询结果，返回null表示通道不对
     * @throws Exception
     */
    public PayQueryResult payQuery(PayQueryForm form) throws Exception {
        checkPayChannel(form.getPayChannel());
        PayQueryResult result = new PayQueryResult();
        if (form.getPayChannel() == PayChannel.ALIPAY) {
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(form.getOutTradeNo());
            model.setQueryOptions(form.getQueryOptions());
            AlipayTradeQueryResponse response = alipayHandler.payQuery(model);
            result.setAlipayResult(response)
                    .setPayChannel(form.getPayChannel())
                    .setOutTradeNo(form.getOutTradeNo())
                    .setTradeNo(response.getTradeNo())
                    .setPayStatus(getPayStatus(form.getPayChannel(), response.getTradeStatus()))
                    .setTotalAmount(getFenFromYuan(response.getTotalAmount()))
                    .setTotalAmountMoney(response.getTotalAmount())
                    .setPayAmount(getFenFromYuan(response.getBuyerPayAmount()))
                    .setPayAmountMoney(response.getPayAmount())
                    .setSuccessTime(response.getSendPayDate().toString())
                    .setWechatResult(null);
        }
        if (form.getPayChannel() == PayChannel.WECHAT) {
            PayQueryReturn response = wechatHandler.payQuery(form.getOutTradeNo());
            result.setWechatResult(response)
                    .setPayChannel(form.getPayChannel())
                    .setOutTradeNo(form.getOutTradeNo())
                    .setTradeNo(response.getTransactionId())
                    .setPayStatus(getPayStatus(form.getPayChannel(), response.getTradeType()))
                    .setTotalAmount(Integer.parseInt(response.getAmount().getTotal() + ""))
                    .setTotalAmountMoney(getYuanFromFen(result.getTotalAmount()))
                    .setPayAmount(Integer.parseInt(response.getAmount().getPayerTotal() + ""))
                    .setPayAmountMoney(getYuanFromFen(result.getPayAmount()))
                    .setSuccessTime(getDateTimeStringFromRFC3339(response.getSuccessTime()))
                    .setAlipayResult(null);
        }
        return result;
    }

    /**
     * 微信支付回调
     *
     * @param notice
     * @return PayNotify
     */
    public PayNotify payNotifyWechat(PayNotice notice) throws Exception {
        PayNotice n = wechatHandler.payNotify(notice);
        PayNotify payNotify = new PayNotify();
        if (n.getEventType().equals(WechatConstant.ORDER_NOTICE_SUCCESS)) {
            payNotify.setCode(0)
                    .setPayChannel(PayChannel.WECHAT)
                    .setStatus(n.getOrigin().getTradeState())
                    .setOutTradeNo(n.getOrigin().getOutTradeNo())
                    .setTradeNo(n.getOrigin().getTransactionId())
                    .setAppId(n.getOrigin().getAppid())
                    .setSellerId(n.getOrigin().getMchid())
                    .setTotalAmount(n.getOrigin().getAmount().getTotal())
                    .setTotalAmountMoney(getYuanFromFen(n.getOrigin().getAmount().getTotal()))
                    .setRefundAmount(0)
                    .setRefundAmountMoney("0.00")
                    .setPayTime(getDateTimeStringFromRFC3339(n.getOrigin().getSuccessTime()))
                    .setNotifyTime(getDateTimeStringFromRFC3339(n.getCreateTime()))
                    .setType(1);
        } else {
            payNotify.setCode(-1);
        }
        return payNotify;
    }

    /**
     * 微信退单回调
     */
    public PayNotify payRefundNotifyWechat(RefundNotice notice) throws Exception {
        RefundNotice n = wechatHandler.payRefundNotify(notice);
        PayNotify notify = new PayNotify();
        if (n.getEventType().equals(WechatConstant.REFUND_NOTICE_SUCCESS)) {
            notify.setPayChannel(PayChannel.WECHAT)
                    .setStatus(n.getOrigin().getRefundStatus())
                    .setOutTradeNo(n.getOrigin().getOutTradeNo())
                    .setTradeNo(n.getOrigin().getTransactionId())
                    .setAppId("")
                    .setSellerId(n.getOrigin().getMchid())
                    .setTotalAmount(n.getOrigin().getAmount().getTotal())
                    .setTotalAmountMoney(getYuanFromFen(n.getOrigin().getAmount().getTotal()))
                    .setRefundAmount(n.getOrigin().getAmount().getRefund())
                    .setRefundAmountMoney(getYuanFromFen(n.getOrigin().getAmount().getRefund()))
                    .setPayTime("")
                    .setNotifyTime(getDateTimeStringFromRFC3339(n.getCreateTime()))
                    .setRefundTime(getDateTimeStringFromRFC3339(n.getOrigin().getSuccessTime()))
                    .setCode(0)
                    .setType(2);
        } else {
            notify.setCode(-1);
        }
        return notify;
    }

    /**
     * 支付宝回调：付款和退单同一个
     *
     * @param maps
     * @return PayNotify
     * @throws Exception
     */
    public PayNotify payNotifyAlipay(Map<String, String[]> maps) throws Exception {
        Map<String, String> params = alipayHandler.getNotifyMaps(maps);
        return getPayNotify(params);
    }

    private PayNotify getPayNotify(Map<String, String> map) {
        PayNotify notify = new PayNotify();
        String tradeStatus = map.get("trade_status");
        if (PayStatus.TRADE_CLOSED.name().equals(tradeStatus) || PayStatus.TRADE_SUCCESS.name().equals(tradeStatus)) {
            notify.setPayChannel(PayChannel.ALIPAY)
                    .setStatus(map.get("trade_status"))
                    .setOutTradeNo(map.get("out_trade_no"))
                    .setTradeNo(map.get("trade_no"))
                    .setAppId(map.get("app_id"))
                    .setSellerId(map.get("seller_id"))
                    .setTotalAmount(getFenFromYuan(map.get("total_amount")))
                    .setTotalAmountMoney(map.get("total_amount"))
                    .setPayTime(map.get("gmt_payment"))
                    .setNotifyTime(map.get("notify_time"))
                    .setCode(0);
            if (PayStatus.TRADE_CLOSED.name().equals(tradeStatus)) {
                notify.setRefundAmount(getFenFromYuan(map.get("refund_fee")))
                        .setRefundAmountMoney(map.get("refund_fee"))
                        .setRefundTime(map.get("gmt_refund"))
                        .setType(2);
            }
            if (PayStatus.TRADE_SUCCESS.name().equals(tradeStatus)) {
                notify.setRefundAmount(0)
                        .setRefundAmountMoney("0.00")
                        .setRefundTime("")
                        .setType(1);
            }
        } else {
            notify.setCode(-1);
        }
        return notify;
    }

    /**
     * 关闭订单
     *
     * @param form 关单参数
     * @throws AlipayApiException
     */
    public void payClose(PayCloseForm form) throws Exception {
        checkPayChannel(form.getPayChannel());
        if (form.getPayChannel() == PayChannel.ALIPAY) {
            alipayHandler.payClose(form.getOutTradeNo());
        }
        if (form.getPayChannel() == PayChannel.WECHAT) {
            wechatHandler.payClose(form.getOutTradeNo());
        }
    }

    /**
     * 退单
     *
     * @param form 退单信息
     * @return 退单结果
     * @throws Exception
     */
    public PayRefundResult payRefund(PayRefundForm form) throws Exception {
        checkPayChannel(form.getPayChannel());
        if (StringUtils.isBlank(form.getOutRefundNo())) {
            form.setOutRefundNo(BaseUtils.getOrderNo());
        }
        PayRefundResult result = new PayRefundResult();
        if (form.getPayChannel() == PayChannel.ALIPAY) {
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", form.getOutTradeNo());
            bizContent.put("refund_amount", getYuanFromFen(form.getRefundAmount()));
            if (StringUtils.isNotBlank(form.getRefundReason())) {
                bizContent.put("refund_reason", form.getRefundReason());
            }
            bizContent.put("out_request_no", form.getOutRefundNo());
            AlipayTradeRefundResponse refund = alipayHandler.payRefund(bizContent);
            if (Objects.equals(refund.getCode(), "10000")) {
                result.setStatus(PayConstant.REFUND_STATUS_SUCCESS);
                PayRefundResultAlipay prra = new PayRefundResultAlipay();
                prra.setStoreName(refund.getStoreName())
                        .setBuyerUserId(refund.getBuyerUserId())
                        .setSendBackFee(refund.getSendBackFee())
                        .setRefundHybAmount(refund.getRefundHybAmount())
                        .setBuyerLogonId(refund.getBuyerLogonId())
                        .setFundChange(refund.getFundChange());
                result.setPayChannel(form.getPayChannel())
                        .setTradeNo(refund.getTradeNo())
                        .setOutTradeNo(refund.getOutTradeNo())
                        .setRefundNo(refund.getTradeNo())
                        .setOutRefundNo(form.getOutRefundNo())
                        .setSuccessTime(getDateTimeStringFromRFC3339(refund.getGmtRefundPay()))
                        .setRefundAmount(getFenFromYuan(refund.getRefundFee()))
                        .setRefundAmountMoney(refund.getRefundFee())
                        .setRefundResultAlipay(prra);
            }
            result.setStatus(refund.getMsg());

        }
        if (form.getPayChannel() == PayChannel.WECHAT) {
            RefundCreateReturn refund = wechatHandler.payRefund(form.getOutTradeNo(), form.getOutRefundNo(),
                    form.getRefundReason(), form.getRefundAmount(), form.getOrderAmount());
            if (Objects.equals(refund.getCode(), "200")) {
                result.setStatus(PayConstant.REFUND_STATUS_SUCCESS);
                PayRefundResultWechat prrw = new PayRefundResultWechat();
                prrw.setAmount(refund.getAmount())
                        .setFundsAccount(refund.getFundsAccount())
                        .setPromotionDetail(refund.getPromotionDetail());
                result.setPayChannel(form.getPayChannel())
                        .setOutTradeNo(refund.getOutTradeNo())
                        .setTradeNo(refund.getTransactionId())
                        .setRefundNo(refund.getRefundId())
                        .setOutRefundNo(form.getOutRefundNo())
                        .setSuccessTime(getDateTimeStringFromRFC3339(refund.getSuccessTime()))
                        .setRefundAmount(refund.getAmount().getRefund())
                        .setRefundAmountMoney(getYuanFromFen(refund.getAmount().getRefund()))
                        .setRefundResultWechat(prrw);
            }
        }
        return result;
    }

    /**
     * 退单查询
     *
     * @param form 商户退单号
     * @return 退单结果
     */
    public PayRefundResult payRefundQuery(PayRefundQueryForm form) throws Exception {
        checkPayChannel(form.getPayChannel());
        PayRefundResult result = new PayRefundResult();
        result.setPayChannel(form.getPayChannel())
                .setOutRefundNo(form.getOutRefundNo())
                .setOutTradeNo(form.getOutTradeNo());
        if (form.getPayChannel() == PayChannel.ALIPAY) {
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", form.getOutTradeNo());
            bizContent.put("out_request_no", form.getOutRefundNo());
            AlipayTradeFastpayRefundQueryResponse refund = alipayHandler.refundQuery(bizContent);
            if (Objects.equals(refund.getCode(), "10000")) {
                result.setStatus(PayConstant.REFUND_STATUS_SUCCESS);
                PayRefundResultAlipay prra = new PayRefundResultAlipay();
                prra.setStoreName("")
                        .setBuyerUserId("")
                        .setSendBackFee(refund.getSendBackFee())
                        .setRefundHybAmount(refund.getRefundHybAmount())
                        .setBuyerLogonId("")
                        .setFundChange("");
                result.setPayChannel(form.getPayChannel())
                        .setTradeNo(refund.getTradeNo())
                        .setOutTradeNo(refund.getOutTradeNo())
                        .setRefundNo(refund.getTradeNo())
                        .setOutRefundNo(form.getOutRefundNo())
                        .setSuccessTime(getDateTimeStringFromRFC3339(refund.getGmtRefundPay()))
                        .setRefundAmount(0)
                        .setRefundAmountMoney("")
                        .setRefundResultAlipay(prra);
            }
        }
        if (form.getPayChannel() != PayChannel.WECHAT) {
            RefundQueryReturn refund = wechatHandler.payRefundQuery(form.getOutRefundNo());
            if (refund.getStatus().equals(PayConstant.REFUND_STATUS_SUCCESS)) {
                result.setStatus(PayConstant.REFUND_STATUS_SUCCESS);
                PayRefundResultWechat prrw = new PayRefundResultWechat();
                prrw.setAmount(refund.getAmount())
                        .setFundsAccount(refund.getFundsAccount())
                        .setPromotionDetail(refund.getPromotionDetail());
                result.setPayChannel(form.getPayChannel())
                        .setOutTradeNo(refund.getOutTradeNo())
                        .setTradeNo(refund.getTransactionId())
                        .setRefundNo(refund.getRefundId())
                        .setOutRefundNo(form.getOutRefundNo())
                        .setSuccessTime(getDateTimeStringFromRFC3339(refund.getSuccessTime()))
                        .setRefundAmount(refund.getAmount().getRefund())
                        .setRefundAmountMoney(getYuanFromFen(refund.getAmount().getRefund()))
                        .setRefundResultWechat(prrw);
            }
        }
        return result;
    }


    /**
     * 获取支付状态
     *
     * @param payChannel 支付通道
     * @param status     状态名称
     * @return 支付状态
     */
    private PayStatus getPayStatus(PayChannel payChannel, String status) {
        if (payChannel == PayChannel.WECHAT) {
            switch (status) {
                case "SUCCESS":
                    return PayStatus.TRADE_SUCCESS;
                case "REFUND":
                    return PayStatus.TRADE_REFUND;
                case "NOTPAY":
                case "USERPAYING":
                    return PayStatus.WAIT_PAY;
                case "CLOSED":
                case "REVOKED":
                    return PayStatus.TRADE_CLOSED;
                case "PAYERROR":
                    return PayStatus.TRADE_FAILED;
                default:
                    return PayStatus.UNKNOWN;
            }
        }
        if (payChannel == PayChannel.ALIPAY) {
            switch (status) {
                case "TRADE_CLOSED":
                    return PayStatus.TRADE_CLOSED;
                case "TRADE_FINISHED":
                    return PayStatus.TRADE_FINISHED;
                case "TRADE_SUCCESS":
                    return PayStatus.TRADE_SUCCESS;
                case "WAIT_BUYER_PAY":
                    return PayStatus.WAIT_PAY;
                default:
                    return PayStatus.UNKNOWN;
            }
        }
        return PayStatus.UNKNOWN;
    }

    /**
     * 校验支付通道和支付类型
     *
     * @param payChannel 支付通道
     * @param payType    支付类型
     * @throws Exception
     */
    private void checkPayChannelAndType(PayChannel payChannel, PayType payType) throws Exception {
        if (payChannel == null) {
            throw new Exception(PayConstant.MSG_NOT_NULL_PAY_CHANNEL);
        }
        if (payType == null) {
            throw new Exception(PayConstant.MSG_NOT_NULL_PAY_TYPE);
        }

    }

    /**
     * 校验支付通道
     *
     * @param payChannel
     * @throws Exception
     */
    private void checkPayChannel(PayChannel payChannel) throws Exception {
        if (payChannel == null) {
            throw new Exception(PayConstant.MSG_NOT_NULL_PAY_CHANNEL);
        }
    }

    private void throwPayChannelException() throws Exception {
        throw new Exception(PayConstant.MSG_PAY_CHANNEL_SUPPORT);
    }

    private void throwPayTypeWechatException() throws Exception {
        throw new Exception(PayConstant.MSG_PAY_TYPE_SUPPORT_WECHAT_ONLY);
    }

    private void throwPayTypeAlipayException() throws Exception {
        throw new Exception(PayConstant.MSG_PAY_TYPE_SUPPORT_APPLY_ONLY);
    }


}
