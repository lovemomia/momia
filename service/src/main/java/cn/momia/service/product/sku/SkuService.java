package cn.momia.service.product.sku;

import cn.momia.service.base.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SkuService extends Service {
    Sku get(long id);
    List<Sku> queryByProduct(long productId);
    Map<Long, List<Sku>> queryByProducts(Collection<Long> productIds);
    boolean lock(long id, int count);
    boolean unlock(long id, int count);
}
