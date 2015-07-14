package cn.momia.mapi.api.v1.dto.base;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

import java.math.BigDecimal;
import java.util.Date;

public class UserCouponDto implements Dto {
    private long id;
    private int couponId;
    private int type;
    private String title;
    private String desc;
    private BigDecimal discount;
    private BigDecimal consumption;
    @JSONField(format = "yyyy-MM-dd hh:mm:ss") private Date startTime;
    @JSONField(format = "yyyy-MM-dd hh:mm:ss") private Date endTime;
    private int status;

    public long getId() {
        return id;
    }

    public int getCouponId() {
        return couponId;
    }

    public int getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getConsumption() {
        return consumption;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public int getStatus() {
        return status;
    }

    public UserCouponDto(JSONObject userCouponJson, JSONObject couponJson) {
        this.id = userCouponJson.getLong("id");
        this.couponId = couponJson.getInteger("id");
        this.type = couponJson.getInteger("type");
        this.title = couponJson.getString("title");
        this.desc = couponJson.getString("desc");
        this.discount = couponJson.getBigDecimal("discount");
        this.consumption = couponJson.getBigDecimal("consumption");
        this.startTime = userCouponJson.getDate("startTime");
        this.endTime = userCouponJson.getDate("endTime");

        int status = userCouponJson.getInteger("status");
        if (status == 1) {
            if (userCouponJson.getBoolean("expired")) status = 3;
        }
        this.status = status;
    }
}
