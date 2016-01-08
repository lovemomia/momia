package cn.momia.service.poi.web.ctrl;

import cn.momia.common.core.http.MomiaHttpResponse;
import cn.momia.common.service.CachedService;
import cn.momia.common.webapp.ctrl.BaseController;
import cn.momia.service.poi.inst.InstitutionService;
import cn.momia.service.poi.place.PlaceService;
import com.google.common.base.Splitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/poi")
public class PoiController extends BaseController {
    @Autowired @Qualifier("cityService") private CachedService cityService;
    @Autowired @Qualifier("regionService") private CachedService regionService;
    @Autowired private PlaceService placeService;
    @Autowired private InstitutionService institutionService;

    @RequestMapping(value = "/city", method = RequestMethod.GET)
    public MomiaHttpResponse listAllCities() {
        return MomiaHttpResponse.SUCCESS(cityService.listAll());
    }

    @RequestMapping(value = "/region", method = RequestMethod.GET)
    public MomiaHttpResponse listAllRegions() {
        return MomiaHttpResponse.SUCCESS(regionService.listAll());
    }

    @RequestMapping(value = "/place/{plid}", method = RequestMethod.GET)
    public MomiaHttpResponse getPlace(@PathVariable(value = "plid") int placeId) {
        return MomiaHttpResponse.SUCCESS(placeService.get(placeId));
    }

    @RequestMapping(value = "/place/list", method = RequestMethod.GET)
    public MomiaHttpResponse listPlaces(@RequestParam String plids) {
        Set<Integer> placeIds = new HashSet<Integer>();
        for (String placeId : Splitter.on(",").trimResults().omitEmptyStrings().split(plids)) {
            placeIds.add(Integer.valueOf(placeId));
        }

        return MomiaHttpResponse.SUCCESS(placeService.list(placeIds));
    }

    @RequestMapping(value = "/inst/{instid}", method = RequestMethod.GET)
    public MomiaHttpResponse getInstitution(@PathVariable(value = "instid") int institutionId) {
        return MomiaHttpResponse.SUCCESS(institutionService.get(institutionId));
    }

    @RequestMapping(value = "/inst/list", method = RequestMethod.GET)
    public MomiaHttpResponse listInstitutions(@RequestParam String instids) {
        Set<Integer> institutionIds = new HashSet<Integer>();
        for (String institutionId : Splitter.on(",").trimResults().omitEmptyStrings().split(instids)) {
            institutionIds.add(Integer.valueOf(institutionId));
        }

        return MomiaHttpResponse.SUCCESS(institutionService.list(institutionIds));
    }
}
