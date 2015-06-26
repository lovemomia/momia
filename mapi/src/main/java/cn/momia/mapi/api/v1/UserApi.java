package cn.momia.mapi.api.v1;

import cn.momia.common.web.http.MomiaHttpParamBuilder;
import cn.momia.common.web.http.MomiaHttpRequest;
import cn.momia.common.web.response.ResponseMessage;
import cn.momia.mapi.api.misc.ProductUtil;
import cn.momia.mapi.api.v1.dto.base.Dto;
import cn.momia.mapi.api.v1.dto.base.OrderDto;
import cn.momia.mapi.api.v1.dto.composite.ListDto;
import cn.momia.mapi.api.v1.dto.base.UserDto;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/v1/user")
public class UserApi extends AbstractApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserApi.class);

    @RequestMapping(value = "/view", method = RequestMethod.GET)
    public ResponseMessage viewUser(@RequestParam long id) {
        MomiaHttpRequest request = MomiaHttpRequest.GET(baseServiceUrl("user", id));

        return executeRequest(request, new Function<Object, Dto>() {
            @Override
            public Dto apply(Object data) {
                return new UserDto.Other((JSONObject) data);
            }
        });
    }

    @RequestMapping(value = "/view/order", method = RequestMethod.GET)
    public ResponseMessage viewOrders(@RequestParam long id) {
        MomiaHttpRequest request = MomiaHttpRequest.GET(baseServiceUrl("user", id, "order"));

        return executeRequest(request);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseMessage getUser(@RequestParam String utoken) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("utoken", utoken);
        MomiaHttpRequest request = MomiaHttpRequest.GET(baseServiceUrl("user"), builder.build());

        return executeRequest(request, new Function<Object, Dto>() {
            @Override
            public Dto apply(Object data) {
                return new UserDto.Own((JSONObject) data);
            }
        });
    }

    @RequestMapping(value = "/order", method = RequestMethod.GET)
    public ResponseMessage getOrdersOfUser(@RequestParam String utoken, @RequestParam int status, @RequestParam String type, @RequestParam int start, @RequestParam final int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("utoken", utoken)
                .add("status", status)
                .add("type", type)
                .add("start", start)
                .add("count", count);
        MomiaHttpRequest request = MomiaHttpRequest.GET(baseServiceUrl("user/order"), builder.build());

        return executeRequest(request, new Function<Object, Dto>() {
            @Override
            public Dto apply(Object data) {
                ListDto orders = new ListDto();

                JSONArray ordersPackJson = (JSONArray) data;
                for (int i = 0; i < ordersPackJson.size(); i++) {
                    try {
                        JSONObject orderPackJson = ordersPackJson.getJSONObject(i);
                        JSONObject orderJson = orderPackJson.getJSONObject("order");
                        JSONObject productJson = orderPackJson.getJSONObject("product");
                        JSONObject skuJson = orderPackJson.getJSONObject("sku");

                        if (productJson == null || skuJson == null) continue;

                        OrderDto orderDto = new OrderDto(orderJson);
                        orderDto.setTitle(productJson.getString("title"));
                        orderDto.setTime(ProductUtil.getSkuTime(skuJson.getJSONArray("properties")));

                        orders.add(orderDto);
                    } catch (Exception e) {
                        LOGGER.error("fail to parse order: {}", ordersPackJson.getJSONObject(i), e);
                    }
                }

                return orders;
            }
        });
    }

    @RequestMapping(value = "/nickname", method = RequestMethod.POST)
    public ResponseMessage updateNickName(@RequestParam String utoken, @RequestParam(value = "nickname") String nickName) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("utoken", utoken)
                .add("nickname", nickName);
        MomiaHttpRequest request = MomiaHttpRequest.PUT(baseServiceUrl("user/nickname"), builder.build());

        return executeRequest(request);
    }

    @RequestMapping(value = "/avatar", method = RequestMethod.POST)
    public ResponseMessage updateAvatar(@RequestParam String utoken, @RequestParam String avatar) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("utoken", utoken)
                .add("avatar", avatar);
        MomiaHttpRequest request = MomiaHttpRequest.PUT(baseServiceUrl("user/avatar"), builder.build());

        return executeRequest(request);
    }

    @RequestMapping(value = "/name", method = RequestMethod.POST)
    public ResponseMessage updateName(@RequestParam String utoken, @RequestParam String name) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("utoken", utoken)
                .add("name", name);
        MomiaHttpRequest request = MomiaHttpRequest.PUT(baseServiceUrl("user/name"), builder.build());

        return executeRequest(request);
    }

    @RequestMapping(value = "/sex", method = RequestMethod.POST)
    public ResponseMessage updateSex(@RequestParam String utoken, @RequestParam String sex) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("utoken", utoken)
                .add("sex", sex);
        MomiaHttpRequest request = MomiaHttpRequest.PUT(baseServiceUrl("user/sex"), builder.build());

        return executeRequest(request);
    }

    @RequestMapping(value = "/birthday", method = RequestMethod.POST)
    public ResponseMessage updateBirthday(@RequestParam String utoken, @RequestParam String birthday) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("utoken", utoken)
                .add("birthday", birthday);
        MomiaHttpRequest request = MomiaHttpRequest.PUT(baseServiceUrl("user/birthday"), builder.build());

        return executeRequest(request);
    }

    @RequestMapping(value = "/city", method = RequestMethod.POST)
    public ResponseMessage updateCity(@RequestParam String utoken, @RequestParam int city) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("utoken", utoken)
                .add("city", city);
        MomiaHttpRequest request = MomiaHttpRequest.PUT(baseServiceUrl("user/city"), builder.build());

        return executeRequest(request);
    }

    @RequestMapping(value = "/address", method = RequestMethod.POST)
    public ResponseMessage updateAddress(@RequestParam String utoken, @RequestParam String address) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("utoken", utoken)
                .add("address", address);
        MomiaHttpRequest request = MomiaHttpRequest.PUT(baseServiceUrl("user/address"), builder.build());

        return executeRequest(request);
    }
}
