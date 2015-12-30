package cn.momia.api.teacher.dto;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.StringUtils;

public class Education {
    public static final Education NOT_EXIST_EDUCATION = new Education();

    private int id;
    private long userId;
    private String school;
    private String major;
    private String level;
    private String time;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean exists() {
        return id > 0;
    }

    @JSONField(serialize = false)
    public boolean isInvalid() {
        return StringUtils.isBlank(school) || StringUtils.isBlank(major) || StringUtils.isBlank(level) || StringUtils.isBlank(time);
    }
}