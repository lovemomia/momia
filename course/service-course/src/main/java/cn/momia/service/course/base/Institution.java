package cn.momia.service.course.base;

public class Institution {
    public static final Institution NOT_EXIST_INSTITUTION = new Institution();

    private int id;
    private String name;
    private String cover;
    private String intro;

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

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public boolean exists() {
        return id > 0;
    }
}