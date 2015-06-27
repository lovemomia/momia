package cn.momia.service.base.product.base;

import java.util.List;

public interface BaseProductService {
    BaseProduct get(long id);
    List<BaseProduct> get(List<Long> ids);
    List<BaseProduct> query(int start, int count, String query);
}