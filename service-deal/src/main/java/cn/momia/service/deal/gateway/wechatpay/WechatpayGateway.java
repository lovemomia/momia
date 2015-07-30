package cn.momia.service.deal.gateway.wechatpay;

import cn.momia.common.misc.XmlUtil;
import cn.momia.common.service.config.Configuration;
import cn.momia.service.deal.gateway.AbstractPaymentGateway;
import cn.momia.service.deal.gateway.CallbackParam;
import cn.momia.service.deal.gateway.PrepayParam;
import cn.momia.service.deal.gateway.PrepayResult;
import cn.momia.service.deal.payment.Payment;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class WechatpayGateway extends AbstractPaymentGateway {
    private static final Logger LOGGER = LoggerFactory.getLogger(WechatpayGateway.class);

    private static final String DATE_FORMAT_STR = "yyyyMMddHHmmss";
    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat(DATE_FORMAT_STR);
    private static final String SUCCESS = "SUCCESS";

    @Override
    protected long getPrepayOutTradeNo(PrepayParam param) {
        String outTradeNo = param.get(WechatpayPrepayFields.OUT_TRADE_NO);
        return Long.valueOf(outTradeNo.substring(0, outTradeNo.length() - DATE_FORMAT_STR.length()));
    }

    @Override
    public PrepayResult doPrepay(PrepayParam param) {
        PrepayResult result = new PrepayResult();

        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost request = createRequest(param);
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException("fail to execute request: " + request);
            }

            String entity = EntityUtils.toString(response.getEntity(), "UTF-8");
            processResponseEntity(result, entity, param.get(WechatpayPrepayFields.TRADE_TYPE));
        } catch (Exception e) {
            LOGGER.error("fail to prepay", e);
            result.setSuccessful(false);
        }

        return result;
    }

    private HttpPost createRequest(PrepayParam param) {
        HttpPost httpPost = new HttpPost(Configuration.getString("Payment.Wechat.PrepayService"));
        httpPost.addHeader(HTTP.CONTENT_TYPE, "application/xml");
        StringEntity entity = new StringEntity(XmlUtil.paramsToXml(param.getAll()), "UTF-8");
        entity.setContentType("application/xml");
        entity.setContentEncoding("UTF-8");
        httpPost.setEntity(entity);

        return httpPost;
    }

    private void processResponseEntity(PrepayResult result, String entity, String tradeType) {
        Map<String, String> params = XmlUtil.xmlToParams(entity);
        String return_code = params.get(WechatpayPrepayFields.RETURN_CODE);
        String result_code = params.get(WechatpayPrepayFields.RESULT_CODE);

        boolean successful = return_code != null && return_code.equalsIgnoreCase(SUCCESS) && result_code != null && result_code.equalsIgnoreCase(SUCCESS);
        result.setSuccessful(successful);

        if (successful) {
            if (!WechatpayUtil.validateSign(params, tradeType)) throw new RuntimeException("fail to prepay, invalid sign");

            if (tradeType.equals("APP")) {
                result.add(WechatpayPrepayFields.PREPAY_RESULT_APP_APPID, Configuration.getString("Payment.Wechat.AppAppId"));
                result.add(WechatpayPrepayFields.PREPAY_RESULT_APP_PARTNERID, Configuration.getString("Payment.Wechat.AppMchId"));
                result.add(WechatpayPrepayFields.PREPAY_RESULT_APP_PREPAYID, params.get(WechatpayPrepayFields.PREPAY_ID));
                result.add(WechatpayPrepayFields.PREPAY_RESULT_APP_PACKAGE, "Sign=WXPay");
                result.add(WechatpayPrepayFields.PREPAY_RESULT_APP_NONCE_STR, WechatpayUtil.createNoncestr(32));
                result.add(WechatpayPrepayFields.PREPAY_RESULT_APP_TIMESTAMP, String.valueOf(new Date().getTime()).substring(0, 10));
                result.add(WechatpayPrepayFields.PREPAY_RESULT_APP_SIGN, WechatpayUtil.sign(result.getAll(), tradeType));
            } else if (tradeType.equals("JSAPI")) {
                result.add(WechatpayPrepayFields.PREPAY_RESULT_JSAPI_APPID, Configuration.getString("Payment.Wechat.JsApiAppId"));
                result.add(WechatpayPrepayFields.PREPAY_RESULT_JSAPI_PACKAGE, "prepay_id=" + params.get(WechatpayPrepayFields.PREPAY_ID));
                result.add(WechatpayPrepayFields.PREPAY_RESULT_JSAPI_NONCE_STR, WechatpayUtil.createNoncestr(32));
                result.add(WechatpayPrepayFields.PREPAY_RESULT_JSAPI_TIMESTAMP, String.valueOf(new Date().getTime()).substring(0, 10));
                result.add(WechatpayPrepayFields.PREPAY_RESULT_JSAPI_SIGN_TYPE, "MD5");
                result.add(WechatpayPrepayFields.PREPAY_RESULT_JSAPI_PAY_SIGN, WechatpayUtil.sign(result.getAll(), tradeType));
            } else {
                throw new RuntimeException("unsupported trade type: " + tradeType);
            }
        } else {
            LOGGER.error("fail to prepay: {}/{}/{}", params.get(WechatpayPrepayFields.RETURN_CODE),
                    params.get(WechatpayPrepayFields.RESULT_CODE),
                    params.get(WechatpayPrepayFields.RETURN_MSG));
        }
    }

    @Override
    protected boolean isPayedSuccessfully(CallbackParam param) {
        String return_code = param.get(WechatpayCallbackFields.RETURN_CODE);
        String result_code = param.get(WechatpayCallbackFields.RESULT_CODE);

        return return_code != null && return_code.equalsIgnoreCase(SUCCESS) &&
                result_code != null && result_code.equalsIgnoreCase(SUCCESS);
    }

    @Override
    protected boolean validateCallbackSign(CallbackParam param) {
        String tradeType = param.get(WechatpayPrepayFields.TRADE_TYPE);
        boolean successful = WechatpayUtil.validateSign(param.getAll(), tradeType);
        if (!successful) LOGGER.warn("invalid sign, order id: {} ", param.get(WechatpayCallbackFields.OUT_TRADE_NO));

        return successful;
    }

    @Override
    protected long getCallbackOutTradeNo(CallbackParam param) {
        String outTradeNo = param.get(WechatpayCallbackFields.OUT_TRADE_NO);
        return Long.valueOf(outTradeNo.substring(0, outTradeNo.length() - DATE_FORMAT_STR.length()));
    }

    @Override
    protected Payment createPayment(CallbackParam param) {
        Payment payment = new Payment();
        payment.setOrderId(getCallbackOutTradeNo(param));
        payment.setPayer(param.get(WechatpayCallbackFields.OPEN_ID));

        Date finishTime;
        try {
            finishTime = DATE_FORMATTER.parse(param.get(WechatpayCallbackFields.TIME_END));
        } catch (ParseException e) {
            finishTime = new Date();
        }
        payment.setFinishTime(finishTime == null ? new Date() : finishTime);

        payment.setPayType(Payment.Type.WECHATPAY);
        payment.setTradeNo(param.get(WechatpayCallbackFields.TRANSACTION_ID));
        payment.setFee(new BigDecimal(param.get(WechatpayCallbackFields.TOTAL_FEE)).divide(new BigDecimal(100)));

        return payment;
    }
}