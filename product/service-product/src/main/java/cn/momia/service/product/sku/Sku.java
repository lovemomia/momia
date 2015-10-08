package cn.momia.service.product.sku;

import cn.momia.common.util.TimeUtil;
import cn.momia.service.product.place.Place;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class Sku implements Serializable {
    private static final Splitter TIME_SPLITTER = Splitter.on("~").trimResults().omitEmptyStrings();
    private static final DateFormat MONTH_DATE_FORMATTER = new SimpleDateFormat("M月d日");
    private static final DateFormat TIME_FORMATTER = new SimpleDateFormat("h:mm");

    public static class Status {
        public static final int ALL = 1;
        public static final int AVALIABLE = 2;
    }

    public static class Type {
        public static final int NORMAL = 0;
        public static final int NO_CEILING = 1;
    }

    public static final Sku NOT_EXIST_SKU = new Sku();

    public static List<Sku> sort(List<Sku> skus) {
        Collections.sort(skus, new Comparator<Sku>() {
            @Override
            public int compare(Sku s1, Sku s2) {
                if (s1.isFull() && s2.isFull()) return 0;
                if (s1.isFull()) return 1;
                if (s2.isFull()) return -1;

                Date time1 = s1.startTime();
                Date time2 = s2.startTime();

                if (time1 == null) return 1;
                if (time2 == null) return -1;

                long timeStamp1 = time1.getTime();
                long timeStamp2 = time2.getTime();

                if (timeStamp1 <= timeStamp2) return -1;
                return 1;
            }
        });

        return skus;
    }

    private Date startTime() {
        List<Date> times = getStartEndTimes();
        if (times.isEmpty()) return null;

        Collections.sort(times);
        return times.get(0);
    }

    public static List<Sku> sortByStartTime(List<Sku> skus) {
        Collections.sort(skus, new Comparator<Sku>() {
            @Override
            public int compare(Sku s1, Sku s2) {
                Date time1 = s1.startTime();
                Date time2 = s2.startTime();

                if (time1 == null) return 1;
                if (time2 == null) return -1;

                long timeStamp1 = time1.getTime();
                long timeStamp2 = time2.getTime();

                if (timeStamp1 <= timeStamp2) return -1;
                return 1;
            }
        });

        return skus;
    }

    public static List<Sku> filterUnavaliable(List<Sku> skus) {
        List<Sku> filteredSkus = new ArrayList<Sku>();

        Date now = new Date();
        for (Sku sku : skus) {
            if (sku.deadline.before(now) || sku.isFinished(now)) continue;
            filteredSkus.add(sku);
        }

        return filteredSkus;
    }

    public static List<Sku> filterFinished(List<Sku> skus) {
        List<Sku> filteredSkus = new ArrayList<Sku>();

        Date now = new Date();
        for (Sku sku : skus) {
            if (sku.isFinished(now)) continue;
            filteredSkus.add(sku);
        }

        return filteredSkus;
    }

    public static List<Sku> filterClosed(List<Sku> skus) {
        List<Sku> filteredSkus = new ArrayList<Sku>();

        Date now = new Date();
        for (Sku sku : skus) {
            if (sku.isClosed(now)) continue;
            filteredSkus.add(sku);
        }

        return filteredSkus;
    }

    private long id;
    private long productId;
    private String desc;
    private int type;
    private boolean anyTime;
    private Date startTime;
    private Date endTime;
    private List<SkuProperty> properties;
    private List<SkuPrice> prices;
    private int limit;
    private boolean needRealName;
    private int stock;
    private int lockedStock;
    private int unlockedStock;
    private Date onlineTime;
    private Date offlineTime;
    private Date deadline;
    private boolean onWeekend;
    private boolean needLeader;
    private long leaderUserId;
    private boolean forNewUser;

    private Place place;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isAnyTime() {
        return anyTime;
    }

    public void setAnyTime(boolean anyTime) {
        this.anyTime = anyTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public List<SkuProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<SkuProperty> properties) {
        this.properties = properties;
    }

    public List<SkuPrice> getPrices() {
        return prices;
    }

    public void setPrices(List<SkuPrice> prices) {
        this.prices = prices;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean isNeedRealName() {
        return needRealName;
    }

    public void setNeedRealName(boolean needRealName) {
        this.needRealName = needRealName;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getLockedStock() {
        return lockedStock;
    }

    public void setLockedStock(int lockedStock) {
        this.lockedStock = lockedStock;
    }

    public int getUnlockedStock() {
        return unlockedStock;
    }

    public void setUnlockedStock(int unlockedStock) {
        this.unlockedStock = unlockedStock;
    }

    public Date getOnlineTime() {
        return onlineTime;
    }

    public void setOnlineTime(Date onlineTime) {
        this.onlineTime = onlineTime;
    }

    public Date getOfflineTime() {
        return offlineTime;
    }

    public void setOfflineTime(Date offlineTime) {
        this.offlineTime = offlineTime;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public boolean isOnWeekend() {
        return onWeekend;
    }

    public void setOnWeekend(boolean onWeekend) {
        this.onWeekend = onWeekend;
    }

    public boolean isNeedLeader() {
        return needLeader;
    }

    public void setNeedLeader(boolean needLeader) {
        this.needLeader = needLeader;
    }

    public long getLeaderUserId() {
        return leaderUserId;
    }

    public void setLeaderUserId(long leaderUserId) {
        this.leaderUserId = leaderUserId;
    }

    public boolean isForNewUser() {
        return forNewUser;
    }

    public void setForNewUser(boolean forNewUser) {
        this.forNewUser = forNewUser;
    }

    public void setPlace(Place place) {
        this.place = place;
    }

    public boolean exists() {
        return id > 0;
    }

    public boolean hasLeader() {
        return leaderUserId > 0;
    }

    public boolean isNoCeiling() {
        return type == Type.NO_CEILING;
    }

    public String getFormatedTime() {
        for (SkuProperty property : properties) {
            if ("time".equalsIgnoreCase(property.getName())) return formatTime(property.getValue());
        }

        return "";
    }

    private String formatTime(String timeValue) {
        if (StringUtils.isBlank(timeValue)) return "";

        List<String> timeStrs = Lists.newArrayList(TIME_SPLITTER.split(timeValue));
        if (timeStrs.isEmpty()) return "";

        Collections.sort(timeStrs);
        List<Date> times = TimeUtil.castToDates(timeStrs);
        if (times.isEmpty()) return "";

        StringBuilder builder = new StringBuilder();

        Date start = times.get(0);
        Date end = times.get(timeStrs.size() - 1);
        if (TimeUtil.isSameDay(start, end)) {
            for (String timeStr : timeStrs) {
                Date time = TimeUtil.castToDate(timeStr);
                if (time != null) {
                    builder.append(formatMonthDateWithWeekDay(time));
                    if (timeStr.contains(":"))
                        builder.append(TimeUtil.getAmPm(time))
                                .append(TIME_FORMATTER.format(time));
                    break;
                }
            }
        } else {
            builder.append(formatMonthDateWithWeekDay(start))
                    .append("~")
                    .append(formatMonthDateWithWeekDay(end));
        }

        return builder.toString();
    }

    private String formatMonthDateWithWeekDay(Date time) {
        StringBuilder builder = new StringBuilder();
        builder.append(MONTH_DATE_FORMATTER.format(time))
                .append("(")
                .append(TimeUtil.getWeekDay(time))
                .append(")");

        return builder.toString();
    }

    public int getPlaceId() {
        if (place != null) return place.getId();

        for (SkuProperty property : properties) {
            if ("place".equalsIgnoreCase(property.getName())) return Integer.valueOf(property.getValue());
        }

        return 0;
    }

    public String getPlaceName() {
        return place == null ? "" : place.getName();
    }

    public int getRegionId() {
        return place == null ? 0 : place.getRegionId();
    }

    public String getAddress() {
        return place == null ? "" : place.getAddress();
    }

    public BigDecimal getMinPrice() {
        if (prices == null || prices.isEmpty()) return new BigDecimal(0);

        BigDecimal minPrice = new BigDecimal(Float.MAX_VALUE);
        for (SkuPrice skuPrice : prices) {
            BigDecimal price = skuPrice.getPrice();
            if (price.compareTo(minPrice) <= 0) minPrice = price;
        }

        return minPrice;
    }

    public BigDecimal getMinOriginalPrice() {
        if (prices == null || prices.isEmpty()) return new BigDecimal(0);

        SkuPrice minSkuPrice = null;
        BigDecimal minPrice = new BigDecimal(Float.MAX_VALUE);
        for (SkuPrice skuPrice : prices) {
            BigDecimal price = skuPrice.getPrice();
            if (price.compareTo(minPrice) <= 0) {
                minSkuPrice = skuPrice;
                minPrice = price;
            }
        }

        return minSkuPrice == null ? new BigDecimal(0) : minSkuPrice.getOrigin();
    }

    public List<Date> getStartEndTimes() {
        for (SkuProperty property : properties) {
            if ("time".equalsIgnoreCase(property.getName())) {
                return TimeUtil.castToDates(Lists.newArrayList(TIME_SPLITTER.split(property.getValue())));
            }
        }

        return new ArrayList<Date>();
    }

    public boolean isFull() {
        return type != 1 && unlockedStock <= 0;
    }

    public boolean isFinished(Date now) {
        return offlineTime.before(now) || (startTime.before(now) && !anyTime);
    }

    public boolean isClosed(Date now) {
        return isFull() || deadline.before(now) || isFinished(now);
    }
}
