package cn.momia.service.course.base;

import cn.momia.common.service.Entity;

public class Teacher implements Entity {
    public static final Teacher NOT_EXIST_TEACHER = new Teacher();

    private int id;
    private String name;
    private String avatar;
    private String education;
    private String experience;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    @Override
    public boolean exists() {
        return id > 0;
    }
}
