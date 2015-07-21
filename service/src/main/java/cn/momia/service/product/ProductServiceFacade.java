package cn.momia.service.product;

import cn.momia.service.product.sku.Sku;

import java.util.Collection;
import java.util.List;

public interface ProductServiceFacade {
    Product get(long id);
    List<Product> get(Collection<Long> ids);
    long queryCount(ProductQuery productQuery);
    List<Product> query(int start, int count, ProductQuery query);
    List<Sku> getSkus(long id);
    Sku getSku(long skuId);
    boolean lockStock(long id, long skuId, int count);
    boolean unlockStock(long id, long skuId, int count);
    boolean join(long productId, int count);
    boolean sold(long id, int count);
}