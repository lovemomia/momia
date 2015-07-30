package cn.momia.mapi.api.v1;

import cn.momia.common.web.http.MomiaHttpParamBuilder;
import cn.momia.common.web.http.MomiaHttpRequest;
import cn.momia.common.web.img.ImageFile;
import cn.momia.common.web.response.ResponseMessage;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/leader")
public class LeaderV1Api extends AbstractV1Api {
    @RequestMapping(value = "/apply", method = RequestMethod.GET)
    public ResponseMessage applyLeader(@RequestParam String utoken, @RequestParam(value = "pid") long productId) {
        if (StringUtils.isBlank(utoken)) return ResponseMessage.BAD_REQUEST;

        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("utoken", utoken)
                .add("pid", productId);
        MomiaHttpRequest request = MomiaHttpRequest.GET(url("leader/apply"), builder.build());

        return executeRequest(request, new Function<Object, Object>() {
            @Override
            public Object apply(Object data) {
                JSONObject leaderApplyJson = (JSONObject) data;
                if (leaderApplyJson.containsKey("desc")) {
                    JSONObject statusDescJson = leaderApplyJson.getJSONObject("desc");
                    statusDescJson.put("image", ImageFile.url(statusDescJson.getString("image")));
                }

                return data;
            }
        });
    }

    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public ResponseMessage getLeaderInfo(@RequestParam String utoken) {
        if (StringUtils.isBlank(utoken)) return ResponseMessage.BAD_REQUEST;

        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("utoken", utoken);
        MomiaHttpRequest request = MomiaHttpRequest.GET(url("leader"), builder.build());

        return executeRequest(request);
    }

    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    public ResponseMessage addLeaderInfo(@RequestParam String utoken, @RequestParam String leader) {
        if (StringUtils.isBlank(utoken) || StringUtils.isBlank(leader)) return ResponseMessage.BAD_REQUEST;

        JSONObject leaderJson = JSON.parseObject(leader);
        leaderJson.put("userId", getUserId(utoken));
        MomiaHttpRequest request = MomiaHttpRequest.POST(url("leader"), leaderJson.toString());

        return executeRequest(request);
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public ResponseMessage updateLeaderInfo(@RequestParam String utoken, @RequestParam String leader) {
        if (StringUtils.isBlank(utoken) || StringUtils.isBlank(leader)) return ResponseMessage.BAD_REQUEST;

        JSONObject leaderJson = JSON.parseObject(leader);
        leaderJson.put("userId", getUserId(utoken));
        MomiaHttpRequest request = MomiaHttpRequest.PUT(url("leader"), leaderJson.toString());

        return executeRequest(request);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResponseMessage deleteLeaderInfo(@RequestParam String utoken) {
        if (StringUtils.isBlank(utoken)) return ResponseMessage.BAD_REQUEST;

        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("utoken", utoken);
        MomiaHttpRequest request = MomiaHttpRequest.DELETE(url("leader"), builder.build());

        return executeRequest(request);
    }
}