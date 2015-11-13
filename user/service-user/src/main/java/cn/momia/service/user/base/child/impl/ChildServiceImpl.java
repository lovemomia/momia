package cn.momia.service.user.base.child.impl;

import cn.momia.common.service.AbstractService;
import cn.momia.service.user.base.child.Child;
import cn.momia.service.user.base.child.ChildService;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChildServiceImpl extends AbstractService implements ChildService {
    @Override
    public long add(final Child child) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                String sql = "INSERT INTO SG_Child (UserId, Avatar, Name, Sex, Birthday, AddTime) VALUES (?, ?, ?, ?, ?, NOW())";
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, child.getUserId());
                ps.setString(2, child.getAvatar());
                ps.setString(3, child.getName());
                ps.setString(4, child.getSex());
                ps.setDate(5, new java.sql.Date(child.getBirthday().getTime()));

                return ps;
            }
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public Child get(long childId) {
        Set<Long> childIds = Sets.newHashSet(childId);
        List<Child> children = list(childIds);

        return children.isEmpty() ? Child.NOT_EXIST_USER_CHILD : children.get(0);
    }

    @Override
    public List<Child> list(Collection<Long> childIds) {
        if (childIds.isEmpty()) return new ArrayList<Child>();

        String sql = "SELECT Id, UserId, Avatar, Name, Sex, Birthday FROM SG_Child WHERE Id IN (" + StringUtils.join(childIds, ",") + ") AND Status=1";
        return queryList(sql, Child.class);
    }

    @Override
    public Map<Long, List<Child>> queryByUsers(Collection<Long> userIds) {
        if (userIds.isEmpty()) return new HashMap<Long, List<Child>>();

        String sql = "SELECT Id FROM SG_Child WHERE UserId IN (" + StringUtils.join(userIds, ",") + ") AND Status=1";
        List<Long> childIds = queryLongList(sql);
        List<Child> children = list(childIds);

        Map<Long, List<Child>> childrenMap = new HashMap<Long, List<Child>>();
        for (long userId : userIds) {
            childrenMap.put(userId, new ArrayList<Child>());
        }
        for (Child child : children) {
            childrenMap.get(child.getUserId()).add(child);
        }

        return childrenMap;
    }

    @Override
    public boolean updateAvatar(long userId, long childId, String avatar) {
        String sql = "UPDATE SG_Child SET Avatar=? WHERE UserId=? AND Id=?";
        return update(sql, new Object[] { avatar, userId, childId });
    }

    @Override
    public boolean updateName(long userId, long childId, String name) {
        String sql = "UPDATE SG_Child SET Name=? WHERE UserId=? AND Id=?";
        return update(sql, new Object[] { name, userId, childId });
    }

    @Override
    public boolean updateSex(long userId, long childId, String sex) {
        String sql = "UPDATE SG_Child SET Sex=? WHERE UserId=? AND Id=?";
        return update(sql, new Object[] { sex, userId, childId });
    }

    @Override
    public boolean updateBirthday(long userId, long childId, Date birthday) {
        String sql = "UPDATE SG_Child SET Birthday=? WHERE UserId=? AND Id=?";
        return update(sql, new Object[] { birthday, userId, childId });
    }

    @Override
    public boolean delete(long userId, long childId) {
        String sql = "UPDATE SG_Child SET Status=0 WHERE UserId=? AND Id=?";
        return update(sql, new Object[] { userId, childId });
    }
}
