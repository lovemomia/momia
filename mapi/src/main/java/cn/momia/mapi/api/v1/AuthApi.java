package cn.momia.mapi.api.v1;

import cn.momia.common.web.http.MomiaHttpParamBuilder;
import cn.momia.common.web.http.MomiaHttpRequest;
import cn.momia.common.web.response.ResponseMessage;
import cn.momia.mapi.api.AbstractApi;
import cn.momia.mapi.api.v1.dto.base.Dto;
import cn.momia.mapi.api.v1.dto.base.UserDto;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthApi extends AbstractApi {
    @RequestMapping(value = "/send", method = RequestMethod.POST)
    public ResponseMessage send(@RequestParam String mobile, @RequestParam String type)  {
        if(StringUtils.isBlank(type)||!(StringUtils.equals(type,"login")||StringUtils.equals(type,"register")))
            return ResponseMessage.FAILED("send code failed, please make sure your type is correct.");

        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("mobile", mobile)
                .add("type", type);
        MomiaHttpRequest request = MomiaHttpRequest.POST(baseServiceUrl("auth/send"), builder.build());

        return executeRequest(request);
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseMessage login(@RequestParam String mobile, @RequestParam String code) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("mobile", mobile)
                .add("code", code);
        MomiaHttpRequest request = MomiaHttpRequest.POST(baseServiceUrl("auth/login"), builder.build());

        return executeRequest(request, new Function<Object, Dto>() {
            @Override
            public Dto apply(Object data) {
                return new UserDto((JSONObject) data);
            }
        });
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ResponseMessage register(@RequestParam String nickName, @RequestParam String mobile, @RequestParam String code) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("nickName", nickName)
                .add("mobile", mobile)
                .add("code", code);
        MomiaHttpRequest request = MomiaHttpRequest.POST(baseServiceUrl("auth/register"), builder.build());

        return executeRequest(request, new Function<Object, Dto>() {
            @Override
            public Dto apply(Object data) {
                return new UserDto((JSONObject) data);
            }
        });
    }
}
