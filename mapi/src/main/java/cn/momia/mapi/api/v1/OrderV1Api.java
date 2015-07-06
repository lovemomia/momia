package cn.momia.mapi.api.v1;

import cn.momia.common.web.http.MomiaHttpParamBuilder;
import cn.momia.common.web.http.MomiaHttpRequest;
import cn.momia.common.web.response.ResponseMessage;
import cn.momia.mapi.api.v1.dto.base.Dto;
import cn.momia.mapi.api.v1.dto.base.OrderDto;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/order")
public class OrderV1Api extends AbstractV1Api {
    @RequestMapping(method = RequestMethod.POST)
    public ResponseMessage placeOrder(@RequestParam String utoken, @RequestParam String order) {
        if (StringUtils.isBlank(utoken) || StringUtils.isBlank(order)) return ResponseMessage.BAD_REQUEST;

        long userId = getUserId(utoken);
        if (userId <= 0) return ResponseMessage.TOKEN_EXPIRED;

        JSONObject orderJson = JSON.parseObject(order);
        orderJson.put("customerId", userId);
        MomiaHttpRequest request = MomiaHttpRequest.POST(dealServiceUrl("order"), orderJson.toString());

        return executeRequest(request, new Function<Object, Dto>() {
            @Override
            public Dto apply(Object data) {
                return new OrderDto((JSONObject) data);
            }
        });
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResponseMessage deleteOrder(@RequestParam String utoken, @RequestParam long id) {
        if (StringUtils.isBlank(utoken) || id <= 0) return ResponseMessage.BAD_REQUEST;

        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("utoken", utoken);
        MomiaHttpRequest request = MomiaHttpRequest.DELETE(dealServiceUrl("order", id), builder.build());

        return executeRequest(request);
    }
}