package cn.momia.api.event;

import cn.momia.api.event.dto.BannerDto;
import cn.momia.api.event.dto.EventDto;
import cn.momia.common.api.AbstractServiceApi;
import cn.momia.common.api.http.MomiaHttpParamBuilder;
import cn.momia.common.api.http.MomiaHttpRequest;
import cn.momia.common.api.util.CastUtil;
import com.alibaba.fastjson.JSONArray;

import java.util.List;

public class EventServiceApi extends AbstractServiceApi {
    public List<BannerDto> listBanners(int cityId, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("city", cityId)
                .add("count", count);
        MomiaHttpRequest request = MomiaHttpRequest.GET(url("event/banner"), builder.build());

        return CastUtil.toList((JSONArray) executeRequest(request), BannerDto.class);
    }

    public List<EventDto> listEvents(int cityId, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("city", cityId)
                .add("count", count);
        MomiaHttpRequest request = MomiaHttpRequest.GET(url("event/event"), builder.build());

        return CastUtil.toList((JSONArray) executeRequest(request), EventDto.class);
    }
}