package cn.momia.api.course;

import cn.momia.common.core.api.HttpServiceApi;
import cn.momia.common.core.http.MomiaHttpParamBuilder;
import cn.momia.common.core.http.MomiaHttpRequestBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class ActivityServiceApi extends HttpServiceApi {
    public long join(int activityId, String mobile, String childName) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("mobile", mobile)
                .add("cname", childName);
        return executeReturnObject(MomiaHttpRequestBuilder.POST(url("/activity/%d/join", activityId), builder.build()), Number.class).longValue();
    }

    public Object prepayAlipay(long entryId, String type) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("eid", entryId)
                .add("type", type);
        return execute(MomiaHttpRequestBuilder.POST(url("/activity/payment/prepay/alipay"), builder.build()));
    }

    public Object prepayWeixin(long entryId, String type, String code) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("eid", entryId)
                .add("type", type);
        if (!StringUtils.isBlank(code)) builder.add("code", code);
        return execute(MomiaHttpRequestBuilder.POST(url("/activity/payment/prepay/weixin"), builder.build()));
    }

    public boolean callbackAlipay(Map<String, String> params) {
        return "OK".equalsIgnoreCase(executeReturnObject(MomiaHttpRequestBuilder.POST(url("/activity/payment/callback/alipay"), params), String.class));
    }

    public boolean callbackWeixin(Map<String, String> params) {
        return "OK".equalsIgnoreCase(executeReturnObject(MomiaHttpRequestBuilder.POST(url("/activity/payment/callback/weixin"), params), String.class));
    }

    public boolean checkPayment(long entryId) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("eid", entryId);
        return executeReturnObject(MomiaHttpRequestBuilder.POST(url("/activity/payment/check"), builder.build()), Boolean.class);
    }
}
