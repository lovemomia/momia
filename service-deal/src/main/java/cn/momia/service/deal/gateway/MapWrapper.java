package cn.momia.service.deal.gateway;

import java.util.HashMap;
import java.util.Map;

public class MapWrapper {
    private Map<String, String> params = new HashMap<String, String>();

    public void add(String key, String value) {
        params.put(key, value);
    }

    public String get(String key) {
        return params.get(key);
    }

    public void addAll(Map<String, String> params) {
        this.params = params;
    }

    public Map<String, String> all() {
        return params;
    }
}
