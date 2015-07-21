package cn.momia.mapi.api.v1.dto.base;

import cn.momia.common.web.misc.SkuUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

import java.math.BigDecimal;
import java.util.Date;

public class SkuDto implements Dto {
    private long productId;
    private long skuId;
    private String desc;
    private int type;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss") private Date startTime;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss") private Date endTime;
    private int limit;
    private boolean needRealName;
    private int stock;
    private BigDecimal minPrice;
    private String time;
    private JSONArray prices;

    public long getProductId() {
        return productId;
    }

    public long getSkuId() {
        return skuId;
    }

    public String getDesc() {
        return desc;
    }

    public int getType() {
        return type;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isNeedRealName() {
        return needRealName;
    }

    public int getStock() {
        return stock;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public String getTime() {
        return time;
    }

    public JSONArray getPrices() {
        return prices;
    }

    public SkuDto(JSONObject skuJson) {
        this.productId = skuJson.getLong("productId");
        this.skuId = skuJson.getLong("id");
        this.desc = skuJson.getString("desc");
        this.type = skuJson.getInteger("type");
        this.startTime = skuJson.getDate("startTime");
        this.endTime = skuJson.getDate("endTime");
        this.limit = skuJson.getInteger("limit");
        this.needRealName = skuJson.getBoolean("needRealName");
        this.stock = skuJson.getInteger("unlockedStock");
        this.minPrice = skuJson.getBigDecimal("minPrice");
        this.time = SkuUtil.getSkuTime(skuJson.getJSONArray("properties"));
        this.prices = skuJson.getJSONArray("prices");

        if (limit < 0) limit = type == 1 ? 1 : 0;
        if (stock < 0) stock = 0;
    }
}
