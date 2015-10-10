package cn.momia.service.base.region.impl;

import cn.momia.common.service.DbAccessService;
import cn.momia.service.base.region.Region;
import cn.momia.service.base.region.RegionService;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionServiceImpl extends DbAccessService implements RegionService {
    private List<Region> regionsCache = new ArrayList<Region>();
    private Map<Integer, Integer> regionsMap = new HashMap<Integer, Integer>();

    @Override
    protected void doReload() {
        final List<Region> newRegionsCache = new ArrayList<Region>();
        final Map<Integer, Integer> newRegionsMap = new HashMap<Integer, Integer>();

        String sql = "SELECT Id, CityId, Name, ParentId FROM SG_Region WHERE Status=1";
        jdbcTemplate.query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                Region region = new Region();
                region.setId(rs.getInt("Id"));
                region.setCityId(rs.getInt("CityId"));
                region.setName(rs.getString("Name"));
                region.setParentId(rs.getInt("ParentId"));
                newRegionsCache.add(region);
                newRegionsMap.put(region.getId(), newRegionsCache.size() - 1);
            }
        });

        regionsCache = newRegionsCache;
        regionsMap = newRegionsMap;
    }

    @Override
    public Region get(int id) {
        if (isOutOfDate()) reload();

        Integer index = regionsMap.get(id);
        if (index == null) return Region.NOT_EXIST_REGION;

        return regionsCache.get(index);
    }

    @Override
    public List<Region> listAll() {
        if (isOutOfDate()) reload();
        return regionsCache;
    }
}
