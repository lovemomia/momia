package cn.momia.service.base.user.impl;

import cn.momia.service.base.city.CityService;
import cn.momia.service.base.user.participant.Participant;
import cn.momia.service.base.user.participant.ParticipantService;
import cn.momia.service.common.DbAccessService;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import cn.momia.service.base.user.User;
import cn.momia.service.base.user.UserService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserServiceImpl extends DbAccessService implements UserService {
    private static final String[] USER_FIELDS = { "id", "token", "nickName", "mobile", "avatar", "name", "sex", "birthday", "cityId", "address", "children" };
    private CityService cityService;
    private ParticipantService participantService;

    public void setParticipantService(ParticipantService participantService) {
        this.participantService = participantService;
    }

    public void setCityService(CityService cityService) {
        this.cityService = cityService;
    }

    @Override
    public User add(String nickName, String mobile, String token) {
        if (!validateMobile(mobile)) return User.DUPLICATE_USER;

        return addUser(nickName, mobile,  token);
    }

    public boolean validateMobile(String mobile) {
        String sql = "SELECT COUNT(1) FROM t_user WHERE mobile=?";
        int count = jdbcTemplate.query(sql, new Object[] { mobile }, new ResultSetExtractor<Integer>() {
            @Override
            public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) return rs.getInt(1);
                return 0;
            }
        });

        return count == 0;
    }

    public User addUser(final String nickName, final String mobile,  final String token) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                String sql = "INSERT INTO t_user(nickName, mobile, token, addTime) VALUES (?, ?, ?, NOW())";
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, nickName);
                ps.setString(2, mobile);
                ps.setString(3, token);

                return ps;
            }
        }, keyHolder);

        return get(keyHolder.getKey().longValue());
    }

    @Override
    public User get(long id) {
        String sql = "SELECT " + joinFields() + " FROM t_user WHERE id=? AND status=1";

        return jdbcTemplate.query(sql, new Object[] { id }, new ResultSetExtractor<User>() {
            @Override
            public User extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) return buildUser(rs);
                return User.NOT_EXIST_USER;
            }
        });
    }

    private String joinFields() {
        return StringUtils.join(USER_FIELDS, ",");
    }

    private User buildUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setToken(rs.getString("token"));
        user.setNickName(rs.getString("nickName"));
        user.setMobile(rs.getString("mobile"));
        user.setAvatar(rs.getString("avatar"));
        user.setName(rs.getString("name"));
        user.setSex(rs.getString("sex"));
        user.setBirthday(rs.getDate("birthday"));
        user.setCity(cityService.get(rs.getInt("cityId")).getName());
        user.setAddress(rs.getString("address"));

        Set<Long> childList = parseChildren(rs.getString("children"));
        Set<Participant> participants = new HashSet<Participant>();
        for(Long child : childList)
           participants.add(participantService.get(child));
        user.setChildren(participants);

        return user;
    }


    private Set<Long> parseChildren(String children) {
        Set<Long> childList = new HashSet<Long>();
        for (String child : Splitter.on(",").trimResults().omitEmptyStrings().split(children)) {
            childList.add(Long.valueOf(child));
        }

        return childList;
    }

    @Override
    public Map<Long, User> get(List<Long> ids) {
        final Map<Long, User> users = new HashMap<Long, User>();
        if (ids.size() <= 0) return users;

        String sql = "SELECT " + joinFields() + " FROM t_user WHERE id IN (" + StringUtils.join(ids, ",") + ") AND status=1";
        jdbcTemplate.query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                User user = buildUser(rs);
                users.put(user.getId(), user);
            }
        });

        return users;
    }

    @Override
    public User getByToken(String token) {
        String sql = "SELECT " + joinFields() + " FROM t_user WHERE token=? AND status=1";

        return jdbcTemplate.query(sql, new Object[] { token }, new ResultSetExtractor<User>() {
            @Override
            public User extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) return buildUser(rs);
                return User.NOT_EXIST_USER;
            }
        });
    }

    @Override
    public User getByMobile(String mobile) {
        String sql = "SELECT " + joinFields() + " FROM t_user WHERE mobile=? AND status=1";

        return jdbcTemplate.query(sql, new Object[] { mobile }, new ResultSetExtractor<User>() {
            @Override
            public User extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) return buildUser(rs);
                return User.NOT_EXIST_USER;
            }
        });
    }

    @Override
    public User getByNickName(String nickName) {
        String sql = "SELECT " + joinFields() + " FROM t_user WHERE nickName=?";

        return jdbcTemplate.query(sql, new Object[]{nickName}, new ResultSetExtractor<User>() {
            @Override
            public User extractData(ResultSet rs) throws SQLException, DataAccessException {
                if(rs.next()) return buildUser(rs);
                return User.NOT_EXIST_USER;
            }
        });
    }

    @Override
    public boolean updateToken(long id, String token) {
        String sql = "UPDATE t_user SET token=? WHERE id=?";

        return update(id, sql, new Object[] { token, id });
    }

    private boolean update(long id, String sql, Object[] args) {
        User user = get(id);
        if (!user.exists()) return false;

        int affectedRowCount = jdbcTemplate.update(sql, args);
        if (affectedRowCount != 1) return false;

        return true;
    }

    @Override
    public boolean updateNickName(long id, String nickName) {
        String sql = "UPDATE t_user SET nickName=? WHERE id=?";

        return update(id, sql, new Object[] { nickName, id });
    }

    @Override
    public boolean updateAvatar(long id, String avatar) {
        String sql = "UPDATE t_user SET avatar=? WHERE id=?";

        return update(id, sql, new Object[] { avatar, id });
    }

    @Override
    public boolean updateName(long id, String name) {
        String sql = "UPDATE t_user SET name=? WHERE id=?";

        return update(id, sql, new Object[] { name, id });
    }

    @Override
    public boolean updateSex(long id, String sex) {
        String sql = "UPDATE t_user SET sex=? WHERE id=?";

        return update(id, sql, new Object[] { sex, id });
    }

    @Override
    public boolean updateBirthday(long id, Date birthday) {
        String sql = "UPDATE t_user SET `birthday`=? WHERE id=?";

        return update(id, sql, new Object[] { birthday, id });
    }

    @Override
    public boolean updateCityId(long id, int cityId) {
        String sql = "UPDATE t_user SET `cityId`=? WHERE id=?";

        return update(id, sql, new Object[] { cityId, id });
    }

    @Override
    public boolean updateAddress(long id, String address) {
        String sql = "UPDATE t_user SET address=? WHERE id=?";

        return update(id, sql, new Object[] { address, id });
    }

    @Override
    public boolean updateChild(long id, Set<Participant> children) {
        String sql = "UPDATE t_user SET children=? WHERE id=?";
        Set<Long> childrenIds = new HashSet<Long>();
        for(Participant child :children)
            childrenIds.add(child.getId());

        return update(id, sql, new Object[] { StringUtils.join(childrenIds, ","), id });
    }
}
