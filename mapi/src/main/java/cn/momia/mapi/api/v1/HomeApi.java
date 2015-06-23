package cn.momia.mapi.api.v1;

import cn.momia.common.web.http.MomiaHttpParamBuilder;
import cn.momia.common.web.http.MomiaHttpRequest;
import cn.momia.common.web.http.MomiaHttpResponseCollector;
import cn.momia.common.web.http.impl.MomiaHttpGetRequest;
import cn.momia.common.web.response.ResponseMessage;
import cn.momia.mapi.api.misc.ProductUtil;
import cn.momia.mapi.api.v1.dto.Dto;
import cn.momia.mapi.api.v1.dto.HomeDto;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/v1/home")
public class HomeApi extends AbstractApi {
    @RequestMapping(method = RequestMethod.GET)
    public ResponseMessage home(@RequestParam(value = "pageindex") final int pageIndex) {
        List<MomiaHttpRequest> requests = buildHomeRequests(pageIndex);

        return executeRequests(requests, new Function<MomiaHttpResponseCollector, Dto>() {
            @Override
            public Dto apply(MomiaHttpResponseCollector collector) {
                HomeDto homeDto = new HomeDto();

                if (pageIndex == 0) {
                    homeDto.banners = new ArrayList<HomeDto.Banner>();
                    homeDto.banners = extractBannerData((JSONArray) collector.getResponse("banners"));
                }

                homeDto.products = new ArrayList<HomeDto.Product>();
                homeDto.products = extractProductsData((JSONArray) collector.getResponse("products"));
                if (homeDto.products.size() == conf.getInt("Home.PageSize")) homeDto.nextpage = pageIndex + 1;

                return homeDto;
            }
        });
    }

    private List<MomiaHttpRequest> buildHomeRequests(int pageIndex) {
        List<MomiaHttpRequest> requests = new ArrayList<MomiaHttpRequest>();
        if (pageIndex == 0) requests.add(buildBannersRequest());
        requests.add(buildProductsRequest(pageIndex));

        return requests;
    }

    private MomiaHttpRequest buildBannersRequest() {
        int count = conf.getInt("Home.BannerCount");
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("count", count);
        return new MomiaHttpGetRequest("banners", true, baseServiceUrl("banner"), builder.build());
    }

    private MomiaHttpRequest buildProductsRequest(int pageIndex) {
        int pageSize = conf.getInt("Home.PageSize");
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("start", String.valueOf(pageIndex * pageSize))
                .add("count", String.valueOf(pageSize));

        return new MomiaHttpGetRequest("products", true, baseServiceUrl("product"), builder.build());
    }

    private List<HomeDto.Banner> extractBannerData(JSONArray bannerArray) {
        List<HomeDto.Banner> banners = new ArrayList<HomeDto.Banner>();

        for (int i = 0; i < bannerArray.size(); i++) {
            JSONObject bannerObject = bannerArray.getJSONObject(i);
            HomeDto.Banner banner = new HomeDto.Banner();
            banner.cover = bannerObject.getString("cover");
            banner.action = bannerObject.getString("action");

            banners.add(banner);
        }

        return banners;
    }

    private List<HomeDto.Product> extractProductsData(JSONArray productArray) {
        List<HomeDto.Product> products = new ArrayList<HomeDto.Product>();

        for (int i = 0; i < productArray.size(); i++) {
            HomeDto.Product product = new HomeDto.Product();

            JSONObject productObject = productArray.getJSONObject(i);
            JSONObject baseProduct = productObject.getJSONObject("product");
            JSONObject place = productObject.getJSONObject("place");
            JSONArray skus = productObject.getJSONArray("skus");

            product.id = baseProduct.getLong("id");
            product.cover = baseProduct.getString("cover");
            product.title = baseProduct.getString("title");
            product.address = place.getString("address");
            product.poi = StringUtils.join(new Object[] { place.getFloat("lng"), place.getFloat("lat") }, ":");
            product.scheduler = ProductUtil.getScheduler(skus);
            product.joined = baseProduct.getInteger("sales");
            product.price = ProductUtil.getPrice(skus);

            products.add(product);
        }

        return products;
    }
}
