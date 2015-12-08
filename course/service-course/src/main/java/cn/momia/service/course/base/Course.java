package cn.momia.service.course.base;

import cn.momia.api.course.dto.CourseSku;
import cn.momia.api.course.dto.CourseSkuPlace;
import cn.momia.common.api.exception.MomiaErrorException;
import cn.momia.common.util.TimeUtil;
import com.alibaba.fastjson.JSONObject;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Course implements Cloneable {
    public static final Course NOT_EXIST_COURSE = new Course();

    public static class ShowType {
        public static final int BASE = 1;
        public static final int FULL = 2;
    }

    public static class Status {
        public static final int OK = 1;
        public static final int SOLD_OUT = 2;
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("M月d日");

    private long id;
    private int type;
    private long parentId;
    private long subjectId;
    private String title;
    private String cover;
    private int minAge;
    private int maxAge;
    private int insurance;
    private int joined;
    private BigDecimal price;
    private String goal;
    private String flow;
    private String tips;
    private String notice;
    private int institutionId;
    private String institution;

    private int status;

    private List<String> imgs;
    private JSONObject book;
    private List<CourseSku> skus = new ArrayList<CourseSku>();

    private String subject;
    private int stock;
    private boolean buyable;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(long subjectId) {
        this.subjectId = subjectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public int getMinAge() {
        return minAge;
    }

    public void setMinAge(int minAge) {
        this.minAge = minAge;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public int getInsurance() {
        return insurance;
    }

    public void setInsurance(int insurance) {
        this.insurance = insurance;
    }

    public int getJoined() {
        return joined;
    }

    public void setJoined(int joined) {
        this.joined = joined;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getFlow() {
        return flow;
    }

    public void setFlow(String flow) {
        this.flow = flow;
    }

    public String getTips() {
        return tips;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public int getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(int institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<String> getImgs() {
        return imgs;
    }

    public void setImgs(List<String> imgs) {
        this.imgs = imgs;
    }

    public JSONObject getBook() {
        return book;
    }

    public void setBook(JSONObject book) {
        this.book = book;
    }

    public List<CourseSku> getSkus() {
        return skus;
    }

    public void setSkus(List<CourseSku> skus) {
        this.skus = skus;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isBuyable() {
        return buyable;
    }

    public void setBuyable(boolean buyable) {
        this.buyable = buyable;
    }

    @Override
    public Course clone() {
        try {
            return (Course) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean exists() {
        return id > 0;
    }

    public Date getStartTime() {
        Date now = new Date();
        List<Date> startTimes = new ArrayList<Date>();
        for (CourseSku sku : skus) {
            if (sku.isAvaliable(now)) startTimes.add(sku.getStartTime());
        }
        Collections.sort(startTimes);

        return startTimes.isEmpty() ? null : startTimes.get(0);
    }

    public Date getEndTime() {
        Date now = new Date();
        List<Date> endTimes = new ArrayList<Date>();
        for (CourseSku sku : skus) {
            if (sku.isAvaliable(now)) endTimes.add(sku.getEndTime());
        }
        Collections.sort(endTimes);

        return endTimes.isEmpty() ? null : endTimes.get(endTimes.size() - 1);
    }

    public int getRegionId() {
        List<Integer> regionIds = new ArrayList<Integer>();
        for (CourseSku sku : skus) {
            CourseSkuPlace place = sku.getPlace();
            int regionId = place.getRegionId();
            if (!regionIds.contains(regionId)) regionIds.add(regionId);
        }

        if (regionIds.isEmpty()) return 0;
        return regionIds.size() > 1 ? -1 : regionIds.get(0);
    }

    public String getAge() {
        if (minAge <= 0 && maxAge <= 0) throw new MomiaErrorException("invalid age of course: " + id);
        if (minAge <= 0) return maxAge + "岁";
        if (maxAge <= 0) return minAge + "岁";
        if (minAge == maxAge) return minAge + "岁";
        return minAge + "-" + maxAge + "岁";
    }

    public String getScheduler() {
        Date now = new Date();
        List<Date> times = new ArrayList<Date>();
        for (CourseSku sku : skus) {
            if (sku.isAvaliable(now)) {
                times.add(sku.getStartTime());
                times.add(sku.getEndTime());
            }
        }
        Collections.sort(times);

        return format(times);
    }

    private String format(List<Date> times) {
        if (times.isEmpty()) return "";
        if (times.size() == 1) {
            Date start = times.get(0);
            return DATE_FORMAT.format(start) + " " + TimeUtil.getWeekDay(start);
        } else {
            Date start = times.get(0);
            Date end = times.get(times.size() - 1);
            if (TimeUtil.isSameDay(start, end)) {
                return DATE_FORMAT.format(start) + " " + TimeUtil.getWeekDay(start);
            } else {
                return DATE_FORMAT.format(start) + "-" + DATE_FORMAT.format(end);
            }
        }
    }

    public String getScheduler(long skuId) {
        List<Date> times = new ArrayList<Date>();
        for (CourseSku sku : skus) {
            if (sku.getId() == skuId) {
                times.add(sku.getStartTime());
                times.add(sku.getEndTime());
            }
        }
        Collections.sort(times);

        return format(times);
    }

    public CourseSkuPlace getPlace(long skuId) {
        for (CourseSku sku : skus) {
            if (sku.getId() == skuId) return sku.getPlace();
        }

        return null;
    }
}
