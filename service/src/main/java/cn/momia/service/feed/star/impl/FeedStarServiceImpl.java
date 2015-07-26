package cn.momia.service.feed.star.impl;

import cn.momia.service.base.DbAccessService;
import cn.momia.service.feed.star.FeedStarService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class FeedStarServiceImpl extends DbAccessService implements FeedStarService {
    @Override
    public int queryUserCount(long feedId) {
        String sql = "SELECT COUNT(DISTINCT userId) FROM t_feed_star WHERE feedId=? AND status=1";

        return jdbcTemplate.query(sql, new Object[] { feedId }, new ResultSetExtractor<Integer>() {
            @Override
            public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    @Override
    public List<Long> queryUserIds(long id, int start, int count) {
        return null;
    }
}
