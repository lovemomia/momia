package cn.momia.service.product.impl;

import cn.momia.common.misc.TimeUtil;
import cn.momia.service.base.DbAccessService;
import cn.momia.service.product.Product;
import cn.momia.service.product.ProductImage;
import cn.momia.service.product.ProductServiceFacade;
import cn.momia.service.product.base.BaseProduct;
import cn.momia.service.product.base.BaseProductService;
import cn.momia.service.product.place.Place;
import cn.momia.service.product.place.PlaceService;
import cn.momia.service.product.sku.Sku;
import cn.momia.service.product.sku.SkuService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductServiceFacadeImpl extends DbAccessService implements ProductServiceFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductServiceFacadeImpl.class);

    private BaseProductService baseProductService;
    private PlaceService placeService;
    private SkuService skuService;

    public void setBaseProductService(BaseProductService baseProductService) {
        this.baseProductService = baseProductService;
    }

    public void setPlaceService(PlaceService placeService) {
        this.placeService = placeService;
    }

    public void setSkuService(SkuService skuService) {
        this.skuService = skuService;
    }

    @Override
    public Product get(long id) {
        return get(id, false);
    }

    @Override
    public Product get(long id, boolean mini) {
        if (id <= 0) return Product.NOT_EXIST_PRODUCT;

        BaseProduct baseProduct = baseProductService.get(id);
        if (!baseProduct.exists()) return Product.NOT_EXIST_PRODUCT;

        Product product = new Product();
        product.setBaseProduct(baseProduct);

        if (!mini) {
            product.setImgs(getProductImgs(baseProduct.getId()));

            Place place = placeService.get(baseProduct.getPlaceId());
            if (!place.exists()) return Product.NOT_EXIST_PRODUCT;

            product.setPlace(place);
            product.setSkus(skuService.queryByProduct(baseProduct.getId()));
        }

        return product;
    }

    private List<ProductImage> getProductImgs(long productId) {
        final List<ProductImage> imgs = new ArrayList<ProductImage>();
        String sql = "SELECT url, width, height FROM t_product_img WHERE productId=? AND status=1";
        jdbcTemplate.query(sql, new Object[] { productId }, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                imgs.add(buildImage(rs));
            }
        });

        return imgs;
    }

    private ProductImage buildImage(ResultSet rs) throws SQLException {
        ProductImage img = new ProductImage();
        img.setUrl(rs.getString("url"));
        img.setWidth(rs.getInt("width"));
        img.setHeight(rs.getInt("height"));

        return img;
    }

    @Override
    public List<Product> get(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<Product>();

        List<BaseProduct> baseProducts = baseProductService.get(ids);
        return buildProducts(baseProducts);
    }

    private List<Product> buildProducts(List<BaseProduct> baseProducts) {
        List<Product> products = new ArrayList<Product>();
        if (baseProducts.isEmpty()) return products;

        List<Long> productIds = new ArrayList<Long>();
        List<Long> placeIds = new ArrayList<Long>();
        for (BaseProduct baseProduct : baseProducts) {
            if (!baseProduct.exists()) continue;
            productIds.add(baseProduct.getId());
            placeIds.add(baseProduct.getPlaceId());
        }

        Map<Long, List<ProductImage>> imgsOfProducts = getProductsImgs(productIds);
        Map<Long, Place> placeOfProducts = placeService.get(placeIds);
        Map<Long, List<Sku>> skusOfProducts = skuService.queryByProducts(productIds);

        for (BaseProduct baseProduct : baseProducts) {
            if (!baseProduct.exists()) continue;
            Product product = new Product();
            product.setBaseProduct(baseProduct);
            product.setImgs(imgsOfProducts.get(baseProduct.getId()));
            product.setPlace(placeOfProducts.get(baseProduct.getPlaceId()));
            product.setSkus(skusOfProducts.get(baseProduct.getId()));

            if (!product.invalid()) products.add(product);
        }

        return products;
    }

    private Map<Long, List<ProductImage>> getProductsImgs(List<Long> productIds) {
        final Map<Long, List<ProductImage>> imgsOfProducts = new HashMap<Long, List<ProductImage>>();
        if (productIds.isEmpty()) return imgsOfProducts;

        try {
            String sql = "SELECT productId, url, width, height FROM t_product_img WHERE productId IN (" + StringUtils.join(productIds, ",") + ") AND status=1";
            jdbcTemplate.query(sql, new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet rs) throws SQLException {
                    long productId = rs.getLong("productId");
                    ProductImage img = buildImage(rs);
                    List<ProductImage> imgs = imgsOfProducts.get(productId);
                    if (imgs == null) {
                        imgs = new ArrayList<ProductImage>();
                        imgsOfProducts.put(productId, imgs);
                    }
                    imgs.add(img);
                }
            });
        } catch (Exception e) {
            LOGGER.error("fail to get imgs of products: {}", productIds, e);
        }

        return imgsOfProducts;
    }

    @Override
    public long queryCount(int cityId) {
        if (cityId < 0) return 0;
        return baseProductService.queryCount(cityId);
    }

    @Override
    public List<Product> query(int cityId, int start, int count) {
        return buildProducts(baseProductService.query(cityId, start, count));
    }

    @Override
    public long queryCountByWeekend(int cityId) {
        if (cityId < 0) return 0;
        return baseProductService.queryCountByWeekend(cityId);
    }

    @Override
    public List<Product> queryByWeekend(int cityId, int start, int count) {
        return buildProducts(baseProductService.queryByWeekend(cityId, start, count));
    }

    @Override
    public long queryCountByMonth(int cityId, int month) {
        if (cityId < 0 || month <= 0 || month > 12) return 0;
        return baseProductService.queryCountByMonth(cityId, TimeUtil.buildMonthStr(month), TimeUtil.buildNextMonthStr(month));
    }

    @Override
    public List<Product> queryByMonth(int cityId, int month) {
        return buildProducts(baseProductService.queryByMonth(cityId, TimeUtil.buildMonthStr(month), TimeUtil.buildNextMonthStr(month)));
    }

    @Override
    public List<Sku> getSkus(long id) {
        if (id <= 0) return new ArrayList<Sku>();
        return skuService.queryByProduct(id);
    }

    @Override
    public Sku getSku(long skuId) {
        if (skuId <= 0) return Sku.NOT_EXIST_SKU;
        return skuService.get(skuId);
    }

    @Override
    public boolean lockStock(long id, long skuId, int count) {
        if (id <= 0 || skuId <= 0 || count <= 0) return false;

        boolean successful = skuService.lock(skuId, count);
        try {
            if (successful) {
                if (isSoldOut(id)) baseProductService.soldOut(id);
                baseProductService.join(id, count);
            }
        } catch (Exception e) {
            LOGGER.error("fail to update sold out/joined status of product: {}", id, e);
        }

        return successful;
    }

    private boolean isSoldOut(long id) {
        if (id <= 0) return true;

        int unlockedStock = 0;
        List<Sku> skus = getSkus(id);
        for (Sku sku : skus) {
            if (sku.getType() == Sku.Type.NO_CEILING) return false;
            unlockedStock += sku.getUnlockedStock();
        }

        return unlockedStock <= 0;
    }

    @Override
    public boolean unlockStock(long id, long skuId, int count) {
        if (id <= 0 || skuId <= 0 || count <= 0) return true;

        try {
            baseProductService.unSoldOut(id);
        } catch (Exception e) {
            LOGGER.error("fail to set sold out status of product: {}", id, e);
        }

        boolean successful = skuService.unlock(skuId, count);
        if (successful) {
            try {
                baseProductService.decreaseJoined(id, count);
            } catch (Exception e) {
                LOGGER.error("fail to decrease joined of product: {}", id, e);
            }
        }

        return successful;
    }

    @Override
    public boolean sold(long id, int count) {
        if (id <= 0 || count <= 0) return true;
        return baseProductService.sold(id, count);
    }
}
