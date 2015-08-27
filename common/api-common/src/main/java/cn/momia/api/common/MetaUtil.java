package cn.momia.api.common;

import cn.momia.api.common.city.City;
import cn.momia.api.common.region.Region;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MetaUtil {
    private static Date lastReloadTime = null;

    private static Map<Integer, City> citiesMap = new HashMap<Integer, City>();
    private static Map<Integer, Region> regionsMap = new HashMap<Integer, Region>();

    public void init() {
        reload();
    }

    private synchronized static void reload() {
        if (!isOutOfDate()) return;

        try {
            citiesMap = new HashMap<Integer, City>();
            for (City city : CommonServiceApi.CITY.getAll()) citiesMap.put(city.getId(), city);

            regionsMap = new HashMap<Integer, Region>();
            for (Region region : CommonServiceApi.REGION.getAll()) regionsMap.put(region.getId(), region);

            lastReloadTime = new Date();
        } catch (Exception e) {
            // do nothing
        }
    }

    private static boolean isOutOfDate() {
        return lastReloadTime == null || lastReloadTime.before(new Date(new Date().getTime() - 24 * 60 * 60 * 1000));
    }

    public static String getCityName(int cityId) {
        if (isOutOfDate()) reload();

        City city = citiesMap.get(cityId);
        return city == null ? "" : city.getName();
    }

    public static String getRegionName(int regionId) {
        if (isOutOfDate()) reload();

        Region region = regionsMap.get(regionId);
        return region == null ? "" : region.getName();
    }
}