package cn.momia.service.base.age.impl;

import cn.momia.common.service.AbstractService;
import cn.momia.service.base.age.AgeRange;
import cn.momia.service.base.age.AgeRangeService;

import java.util.ArrayList;
import java.util.List;

public class AgeRangeServiceImpl extends AbstractService implements AgeRangeService {
    private List<AgeRange> ageRangesCache = new ArrayList<AgeRange>();

    @Override
    protected void doReload() {
        String sql = "SELECT Id, `Min`, `Max` FROM SG_AgeRange WHERE Status=1";
        ageRangesCache = queryList(sql, AgeRange.class);
    }

    @Override
    public List<AgeRange> listAll() {
        if (isOutOfDate()) reload();
        return ageRangesCache;
    }
}
