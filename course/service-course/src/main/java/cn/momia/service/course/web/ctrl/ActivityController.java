package cn.momia.service.course.web.ctrl;

import cn.momia.api.user.SmsServiceApi;
import cn.momia.common.core.exception.MomiaErrorException;
import cn.momia.common.core.http.MomiaHttpResponse;
import cn.momia.common.core.platform.Platform;
import cn.momia.common.deal.gateway.CallbackParam;
import cn.momia.common.deal.gateway.CallbackResult;
import cn.momia.common.deal.gateway.PayType;
import cn.momia.common.deal.gateway.PaymentGateway;
import cn.momia.common.deal.gateway.PrepayParam;
import cn.momia.common.deal.gateway.PrepayResult;
import cn.momia.common.deal.gateway.factory.CallbackParamFactory;
import cn.momia.common.deal.gateway.factory.PaymentGatewayFactory;
import cn.momia.common.webapp.config.Configuration;
import cn.momia.common.webapp.ctrl.BaseController;
import cn.momia.common.webapp.util.RequestUtil;
import cn.momia.api.course.activity.Activity;
import cn.momia.api.course.activity.ActivityEntry;
import cn.momia.service.course.activity.ActivityService;
import cn.momia.service.course.activity.Payment;
import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
@RequestMapping(value = "/activity")
public class ActivityController extends BaseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityController.class);

    @Autowired private SmsServiceApi smsServiceApi;
    @Autowired private ActivityService activityService;

    @RequestMapping(value = "/{aid}", method = RequestMethod.GET)
    public MomiaHttpResponse get(@PathVariable(value = "aid") int activityId) {
        Activity activity = activityService.getActivity(activityId);
        if (!activity.exists()) return MomiaHttpResponse.FAILED("无效的活动");
        return MomiaHttpResponse.SUCCESS(activity);
    }

    @RequestMapping(value = "/entry/{eid}", method = RequestMethod.GET)
    public MomiaHttpResponse getEntry(@PathVariable(value = "eid") long entryId) {
        ActivityEntry activityEntry = activityService.getActivityEntry(entryId);
        if (!activityEntry.exists()) return MomiaHttpResponse.FAILED("无效的报名信息");
        return MomiaHttpResponse.SUCCESS(activityEntry);
    }

    @RequestMapping(value = "/{aid}/entry", method = RequestMethod.GET)
    public MomiaHttpResponse getEntry(@PathVariable(value = "aid") int activityId, @RequestParam String mobile, @RequestParam(value = "cname") String childName) {
        ActivityEntry activityEntry = activityService.getActivityEntry(activityId, mobile, childName);
        if (!activityEntry.exists()) return MomiaHttpResponse.FAILED("无效的报名信息");
        return MomiaHttpResponse.SUCCESS(activityEntry);
    }

    @RequestMapping(value = "/{aid}/join", method = RequestMethod.POST)
    public MomiaHttpResponse join(@PathVariable(value = "aid") int activityId,
                                  @RequestParam String mobile,
                                  @RequestParam(value = "cname") String childName,
                                  @RequestParam String relation) {
        if (activityService.joined(activityId, mobile, childName)) return MomiaHttpResponse.FAILED("您已为 " + childName + " 报过名，无需重复报名");

        Activity activity = activityService.getActivity(activityId);
        if (!activity.exists()) return MomiaHttpResponse.FAILED("活动不存在");

        long entryId = activityService.join(activityId, mobile, childName, relation, activity.isNeedPay() ? ActivityEntry.Status.NOT_PAYED : ActivityEntry.Status.PAYED);
        return MomiaHttpResponse.SUCCESS(activity.isNeedPay() ? entryId : 0);
    }

    @RequestMapping(value = "/payment/prepay/alipay", method = RequestMethod.POST)
    public MomiaHttpResponse prepayAlipay(HttpServletRequest request) {
        return prepay(request, PayType.ALIPAY);
    }

    private MomiaHttpResponse prepay(HttpServletRequest request, int payType) {
        long entryId = Long.valueOf(request.getParameter("eid"));
        ActivityEntry activityEntry = activityService.getActivityEntry(entryId);
        if (!activityEntry.exists()) return MomiaHttpResponse.FAILED("您还没有报名");

        Activity activity = activityService.getActivity(activityEntry.getActivityId());
        if (!activity.exists()) return MomiaHttpResponse.FAILED("无效的活动");
        if (!activity.isNeedPay()) return MomiaHttpResponse.FAILED("本次活动是免费的");

        if (!activityService.prepay(entryId)) return MomiaHttpResponse.FAILED;

        PrepayParam prepayParam = buildPrepayParam(request, activity, activityEntry, payType);
        PaymentGateway gateway = PaymentGatewayFactory.create(payType);
        PrepayResult prepayResult = gateway.prepay(prepayParam);

        if (!prepayResult.isSuccessful()) return MomiaHttpResponse.FAILED;
        return MomiaHttpResponse.SUCCESS(prepayResult);
    }

    private PrepayParam buildPrepayParam(HttpServletRequest request, Activity activity, ActivityEntry activityEntry, int payType) {
        PrepayParam prepayParam = new PrepayParam();

        prepayParam.setPlatform(extractClientType(request, payType));
        prepayParam.setOutTradeNo("act" + activityEntry.getId());
        prepayParam.setProductId(activity.getId());
        prepayParam.setProductTitle(activity.getTitle());
        prepayParam.setProductUrl(Configuration.getString("Wap.ActivityUrl") + activity.getId());
        prepayParam.setPaymentResultUrl(Configuration.getString("Wap.ActivityPaymentResultUrl") + activityEntry.getId());

        switch (payType) {
            case PayType.ALIPAY:
                prepayParam.setTotalFee(activity.getPrice());
                break;
            case PayType.WEIXIN:
                prepayParam.setTotalFee(new BigDecimal(activity.getPrice().multiply(new BigDecimal(100)).intValue()));
                break;
            default: throw new MomiaErrorException("无效的支付类型: " + payType);
        }

        prepayParam.addAll(extractParams(request));
        prepayParam.add("userIp", RequestUtil.getRemoteIp(request));

        return prepayParam;
    }

    private int extractClientType(HttpServletRequest request, int payType) {
        switch (payType) {
            case PayType.ALIPAY:
                String type = request.getParameter("type");
                if ("app".equalsIgnoreCase(type)) return Platform.APP;
                else if ("wap".equalsIgnoreCase(type)) return Platform.WAP;
                else throw new MomiaErrorException("not supported type: " + type);
            case PayType.WEIXIN:
                String tradeType = request.getParameter("type");
                if ("APP".equalsIgnoreCase(tradeType)) return Platform.APP;
                else if ("JSAPI".equalsIgnoreCase(tradeType)) return Platform.WAP;
                else throw new MomiaErrorException("not supported trade type: " + tradeType);
            default: return 0;
        }
    }

    @RequestMapping(value = "/payment/prepay/weixin", method = RequestMethod.POST)
    public MomiaHttpResponse prepayWeixin(HttpServletRequest request) {
        return prepay(request, PayType.WEIXIN);
    }

    @RequestMapping(value = "/payment/callback/alipay", method = RequestMethod.POST)
    public MomiaHttpResponse alipayCallback(HttpServletRequest request) {
        return callback(request, PayType.ALIPAY);
    }

    private MomiaHttpResponse callback(HttpServletRequest request, int payType) {
        CallbackParam callbackParam = CallbackParamFactory.create(extractParams(request), payType);
        PaymentGateway gateway = PaymentGatewayFactory.create(payType);
        CallbackResult callbackResult = gateway.callback(callbackParam, new Function<CallbackParam, Boolean>() {
            @Override
            public Boolean apply(CallbackParam callbackParam) {
                return doCallback(callbackParam);
            }
        });

        if (callbackResult.isSuccessful()) return MomiaHttpResponse.SUCCESS("OK");
        return MomiaHttpResponse.SUCCESS("FAIL");
    }

    private boolean doCallback(CallbackParam callbackParam) {
        if (!callbackParam.isPayedSuccessfully()) return true;

        long orderId = callbackParam.getOrderId();
        ActivityEntry activityEntry = activityService.getActivityEntry(orderId);
        if (!activityEntry.exists()) {
            // TODO 自动退款
            return true;
        }

        if (activityEntry.isPayed()) {
            // TODO 判断是否重复付款，是则退款
            return true;
        }

        if (!finishPayment(createPayment(callbackParam))) return false;

        notifyUser(callbackParam.getOrderId());

        return true;
    }

    private Payment createPayment(CallbackParam callbackParam) {
        Payment payment = new Payment();
        payment.setOrderId(callbackParam.getOrderId());
        payment.setPayer(callbackParam.getPayer());
        payment.setFinishTime(callbackParam.getFinishTime());
        payment.setPayType(callbackParam.getPayType());
        payment.setTradeNo(callbackParam.getTradeNo());
        payment.setFee(callbackParam.getTotalFee());

        return payment;
    }

    private boolean finishPayment(Payment payment) {
        return activityService.pay(payment);
    }

    private void notifyUser(long entryId) {
        try {
            ActivityEntry activityEntry = activityService.getActivityEntry(entryId);
            if (!activityEntry.exists()) return;
            Activity activity = activityService.getActivity(activityEntry.getActivityId());
            if (!activity.exists()) return;

            smsServiceApi.notify(activityEntry.getMobile(), activity.getMessage() + activityEntry.getExtraMessage());
        } catch (Exception e) {
            LOGGER.error("fail to notify for activity entry: {}", entryId, e);
        }
    }

    @RequestMapping(value = "/payment/callback/weixin", method = RequestMethod.POST)
    public MomiaHttpResponse wechatpayCallback(HttpServletRequest request) {
        return callback(request, PayType.WEIXIN);
    }

    @RequestMapping(value = "/payment/check", method = RequestMethod.POST)
    public MomiaHttpResponse check(@RequestParam(value = "eid") long entryId) {
        ActivityEntry activityEntry = activityService.getActivityEntry(entryId);
        return MomiaHttpResponse.SUCCESS(activityEntry.exists() && activityEntry.isPayed());
    }
}
