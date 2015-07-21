package cn.momia.service.user.impl;

import cn.momia.common.misc.ValidateUtil;
import cn.momia.common.web.exception.MomiaFailedException;
import cn.momia.common.web.secret.SecretKey;
import cn.momia.service.user.UserServiceFacade;
import cn.momia.service.user.base.User;
import cn.momia.service.user.base.UserService;
import cn.momia.service.user.participant.Participant;
import cn.momia.service.user.participant.ParticipantService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class UserServiceFacadeImpl implements UserServiceFacade {
    private UserService userService;
    private ParticipantService participantService;

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setParticipantService(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @Override
    public boolean exists(String field, String value) {
        return !StringUtils.isBlank(field) && !StringUtils.isBlank(value) && userService.exists(field, value);
    }

    @Override
    public User register(String nickName, String mobile, String password) {
        if (StringUtils.isBlank(nickName) ||
                ValidateUtil.isInvalidMobile(mobile) ||
                StringUtils.isBlank(password)) return User.NOT_EXIST_USER;

        long userId = userService.add(nickName, mobile, password, generateToken(mobile));
        return userService.get(userId);
    }

    private String generateToken(String mobile) {
        return DigestUtils.md5Hex(StringUtils.join(new String[] { mobile, new Date().toString(), SecretKey.get() }, "|"));
    }

    @Override
    public User login(String mobile, String password) {
        if (ValidateUtil.isInvalidMobile(mobile) || StringUtils.isBlank(password)) return User.NOT_EXIST_USER;
        if (!userService.validatePassword(mobile, password)) return User.NOT_EXIST_USER;

        return userService.getByMobile(mobile);
    }

    @Override
    public User getUser(long userId) {
        if (userId <= 0) return User.NOT_EXIST_USER;
        return userService.get(userId);
    }

    @Override
    public User getUserByToken(String token) {
        if (StringUtils.isBlank(token)) return User.NOT_EXIST_USER;
        return userService.getByToken(token);
    }

    @Override
    public User getUserByMobile(String mobile) {
        if (ValidateUtil.isInvalidMobile(mobile)) return User.NOT_EXIST_USER;
        return userService.getByMobile(mobile);
    }

    @Override
    public List<User> getUsers(Collection<Long> userIds) {
        return (List<User>) userService.get(userIds).values();
    }

    @Override
    public boolean updateUserNickName(long userId, String nickName) {
        if (userService.exists("nickName", nickName) || StringUtils.isBlank(nickName)) return false;
        return userService.updateNickName(userId, nickName);
    }

    @Override
    public boolean updateUserAvatar(long userId, String avatar) {
        if (StringUtils.isBlank(avatar)) return false;
        return userService.updateAvatar(userId, avatar);
    }

    @Override
    public boolean updateUserName(long userId, String name) {
        if (StringUtils.isBlank(name)) return false;
        return userService.updateName(userId, name);
    }

    @Override
    public boolean updateUserSex(long userId, String sex) {
        if (StringUtils.isBlank(sex)) return false;
        return userService.updateSex(userId, sex);
    }

    @Override
    public boolean updateUserBirthday(long userId, Date birthday) {
        if (birthday == null) return false;
        return userService.updateBirthday(userId, birthday);
    }

    @Override
    public boolean updateUserCityId(long userId, int cityId) {
        if (cityId < 0) return false;
        return userService.updateCityId(userId, cityId);
    }

    @Override
    public boolean updateUserAddress(long userId, String address) {
        if (StringUtils.isBlank(address)) return false;
        return userService.updateAddress(userId, address);
    }

    @Override
    public boolean updateUserChildren(long userId, Set<Long> children) {
        return userService.updateChildren(userId, children);
    }

    @Override
    public User updateUserPassword(String mobile, String password) {
        if (ValidateUtil.isInvalidMobile(mobile) || StringUtils.isBlank(password)) return User.NOT_EXIST_USER;

        User user = userService.getByMobile(mobile);
        if (user.exists() && !userService.updatePassword(user.getId(), mobile, password)) throw new MomiaFailedException("更改密码失败");

        return user;
    }

    @Override
    public long addChild(Participant child) {
        return addParticipant(child);
    }

    @Override
    public Participant getChild(long userId, long childId) {
        return getParticipant(userId, childId);
    }

    @Override
    public List<Participant> getChildren(Collection<Long> childIds) {
        return getParticipants(childIds);
    }

    @Override
    public boolean updateChildName(long userId, long childId, String name) {
        if (userId <= 0 || childId <= 0 || StringUtils.isBlank(name)) return false;
        return participantService.updateName(userId, childId, name);
    }

    @Override
    public boolean updateChildSex(long userId, long childId, String sex) {
        if (userId <= 0 || childId <= 0 || StringUtils.isBlank(sex)) return false;
        return participantService.updateSex(userId, childId, sex);
    }

    @Override
    public boolean updateChildBirthday(long userId, long childId, Date birthday) {
        if (userId <= 0 || childId <= 0 || birthday == null) return false;
        return participantService.updateBirthday(userId, childId, birthday);
    }

    @Override
    public long addParticipant(Participant participant) {
        if (participant.isInvalid()) return 0;
        return participantService.add(participant);
    }

    @Override
    public Participant getParticipant(long userId, long participantId) {
        if (userId <= 0 || participantId <= 0) return Participant.NOT_EXIST_PARTICIPANT;
        return participantService.get(participantId);
    }

    @Override
    public List<Participant> getParticipants(Collection<Long> participantIds) {
        return (List<Participant>) participantService.get(participantIds).values();
    }

    @Override
    public List<Participant> getParticipantsByUser(long userId) {
        if (userId <= 0) return new ArrayList<Participant>();
        return participantService.getByUser(userId);
    }

    @Override
    public boolean updateParticipant(Participant participant) {
        if (participant.isInvalid()) return false;
        return participantService.update(participant);
    }

    @Override
    public boolean deleteParticipant(long userId, long participantId) {
        if (userId <= 0 || participantId <= 0) return false;
        return participantService.delete(userId, participantId);
    }
}