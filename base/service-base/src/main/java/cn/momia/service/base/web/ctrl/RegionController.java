package cn.momia.service.base.web.ctrl;

import cn.momia.common.api.http.MomiaHttpResponse;
import cn.momia.common.webapp.ctrl.BaseController;
import cn.momia.service.base.city.City;
import cn.momia.service.base.facade.CommonServiceFacade;
import cn.momia.service.base.region.Region;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/region")
public class RegionController extends BaseController {
    @Autowired private CommonServiceFacade commonServiceFacade;

    @RequestMapping(method = RequestMethod.GET)
    public MomiaHttpResponse list() {
        return MomiaHttpResponse.SUCCESS(commonServiceFacade.getAllRegions());
    }

    @RequestMapping(value = "/district/tree", method = RequestMethod.GET)
    public MomiaHttpResponse getDistrictTree() {
        List<City> cities = commonServiceFacade.getAllCities();
        List<Region> regions = commonServiceFacade.getAllRegions();
        Map<Integer, List<Region>> districtsOfCities = new HashMap<Integer, List<Region>>();
        for (Region region : regions) {
            if (region.getParentId() > 0) continue;

            int cityId = region.getCityId();
            List<Region> districts = districtsOfCities.get(cityId);
            if (districts == null) {
                districts = new ArrayList<Region>();
                districtsOfCities.put(cityId, districts);
            }
            districts.add(region);
        }

        JSONArray treeJson = new JSONArray();
        for (City city : cities) {
            JSONObject cityDistrictsJson = new JSONObject();
            cityDistrictsJson.put("city", city);
            cityDistrictsJson.put("districts", districtsOfCities.get(city.getId()));

            treeJson.add(cityDistrictsJson);
        }

        return MomiaHttpResponse.SUCCESS(treeJson);
    }
}