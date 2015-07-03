package cn.momia.mapi.api.v1.dto.base;

import cn.momia.mapi.api.v1.dto.composite.ListDto;
import cn.momia.mapi.img.ImageFile;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class UserDto implements Dto {
    public static class Other extends UserDto {
        public Other(JSONObject userJson) {
            super(userJson);
        }

        public String getToken() {
            return "";
        }

        public String getAddress() {
            return "";
        }
    }

    private String token;
    private String nickName;
    private String mobile;
    private String avatar;
    private String name;
    private String sex;
    @JSONField(format = "yyyy-MM-dd") private Date birthday;
    private String city;
    private String address;
    private ListDto children;

    public String getToken() {
        return token;
    }

    public String getNickName() {
        return nickName;
    }

    public String getMobile() {
        return mobile;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getName() {
        return name;
    }

    public String getSex() {
        return sex;
    }

    public Date getBirthday() {
        return birthday;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public ListDto getChildren() {
        return children;
    }

    public UserDto(JSONObject userJson) {
        this.token = userJson.getString("token");
        this.nickName = userJson.getString("nickName");
        this.mobile = encryptMobile(userJson.getString("mobile"));
        this.avatar = ImageFile.url(userJson.getString("avatar"));
        this.name = userJson.getString("name");
        this.sex = userJson.getString("sex");
        this.birthday = userJson.getDate("birthday");
        this.city = userJson.getString("city");
        this.address = userJson.getString("address");
        JSONArray participantsJson  = userJson.getJSONArray("children");

        ListDto participants = new ListDto();

        for(int i=0; i<participantsJson.size(); i++) {
            JSONObject participantJson = participantsJson.getJSONObject(i);
            participants.add(new ParticipantDto(participantJson));
        }
        this.children = participants;



    }

    private String encryptMobile(String mobile) {
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

}
