package cn.momia.service.base.user.participant.impl;

import cn.momia.service.common.DbAccessService;
import cn.momia.service.base.user.participant.Participant;
import cn.momia.service.base.user.participant.ParticipantService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParticipantServiceImpl extends DbAccessService implements ParticipantService {
    @Override
    public long add(final Participant participant) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                String sql = "INSERT INTO t_user_participant(userId, name, sex, birthday, idType,idNo, addTime) VALUES(?, ?, ?, ?, ?, ?, NOW())";
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, participant.getUserId());
                ps.setString(2, participant.getName());
                ps.setString(3, participant.getSex());
                ps.setDate(4, new java.sql.Date(participant.getBirthday().getTime()));
                ps.setInt(5, participant.getIdType());
                ps.setString(6, participant.getIdNo());

                return ps;
            }
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    @Override
    public boolean update(Participant participant) {
        String sql = "UPDATE t_user_participant SET name=?, sex=?, birthday=?, idType=?, idNo=? WHERE id=? AND userId=?";
        int updateCount = jdbcTemplate.update(sql, new Object[] { participant.getName(), participant.getSex(), participant.getBirthday(), participant.getIdType(), participant.getIdNo(), participant.getId(), participant.getUserId() });

        return updateCount == 1;
    }

    @Override
    public boolean delete(long id, long userId) {
        String sql = "UPDATE t_user_participant SET status=0 WHERE id=? AND userId=?";
        int affectedRowCount = jdbcTemplate.update(sql, new Object[] { id, userId });
        if (affectedRowCount != 1) return false;

        return true;
    }

    @Override
    public Participant get(long id) {
        String sql = "SELECT id, userId, name, sex, birthday, idType, idNo FROM t_user_participant WHERE id=? AND status=1";

        return jdbcTemplate.query(sql, new Object[] { id }, new ResultSetExtractor<Participant>() {
            @Override
            public Participant extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) return buildParticipant(rs);
                return Participant.NOT_EXIST_PARTICIPANT;
            }
        });
    }

    public Participant buildParticipant(ResultSet rs) throws SQLException {
        Participant participant = new Participant();
        participant.setId(rs.getLong("id"));
        participant.setUserId(rs.getLong("userId"));
        participant.setName(rs.getString("name"));
        participant.setSex(rs.getString("sex"));
        participant.setBirthday(rs.getDate("birthday"));
        participant.setIdType(rs.getInt("idType"));
        participant.setIdNo(rs.getString("idNo"));

        return participant;
    }

    @Override
    public Map<Long, Participant> get(List<Long> ids) {
        final Map<Long, Participant> participants = new HashMap<Long, Participant>();
        if (ids.size() <= 0) return participants;

        String sql = "SELECT id, userId, name, sex, birthday, idType, idNo FROM t_user_participant WHERE id IN (" + StringUtils.join(ids, ",") + ") AND status=1";
        jdbcTemplate.query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                Participant participant = buildParticipant(rs);
                participants.put(participant.getId(), participant);
            }
        });

        return participants;
    }

    @Override
    public List<Participant> getByUser(long userId) {
        final List<Participant> participants = new ArrayList<Participant>();

        String sql = "SELECT id, userId, name, sex, birthday, idType, idNo FROM t_user_participant WHERE userId=? AND status=1";
        jdbcTemplate.query(sql, new Object[] { userId }, new RowCallbackHandler() {

            @Override
            public void processRow(ResultSet rs) throws SQLException {
                participants.add(buildParticipant(rs));
            }
        });

        return participants;
    }
}
