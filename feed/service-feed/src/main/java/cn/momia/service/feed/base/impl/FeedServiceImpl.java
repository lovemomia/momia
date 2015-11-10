package cn.momia.service.feed.base.impl;

import cn.momia.common.api.exception.MomiaFailedException;
import cn.momia.common.service.DbAccessService;
import cn.momia.service.feed.base.Feed;
import cn.momia.service.feed.base.FeedService;
import cn.momia.service.feed.base.FeedTag;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeedServiceImpl extends DbAccessService implements FeedService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedServiceImpl.class);

    @Override
    public boolean isFollowed(long ownUserId, long otherUserId) {
        String sql = "SELECT COUNT(1) FROM SG_UserFollow WHERE UserId=? AND FollowedId=? AND Status=1";
        return queryInt(sql, new Object[] { ownUserId, otherUserId }) > 0;
    }

    @Override
    public boolean follow(long ownUserId, long otherUserId) {
        String sql = "INSERT INTO SG_UserFollow(UserId, FollowedId, AddTime) VALUES (?, ?, NOW())";
        return update(sql, new Object[] { ownUserId, otherUserId });
    }

    @Override
    public boolean isOfficialUser(long userId) {
        String sql = "SELECT COUNT(1) FROM SG_FeedOfficialUser WHERE UserId=? AND Status=1";
        return queryInt(sql, new Object[] { userId }) > 0;
    }

    @Override
    public long add(final Feed feed) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                String sql = "INSERT INTO SG_Feed(`Type`, UserId, Content, TagId, SubjectId, CourseId, CourseTitle, Lng, Lat, AddTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, feed.getType());
                ps.setLong(2, feed.getUserId());
                ps.setString(3, feed.getContent());
                ps.setLong(4, feed.getTagId());
                ps.setLong(5, feed.getSubjectId());
                ps.setLong(6, feed.getCourseId());
                ps.setString(7, feed.getCourseTitle());
                ps.setDouble(8, feed.getLng());
                ps.setDouble(9, feed.getLat());

                return ps;
            }
        }, keyHolder);

        long feedId = keyHolder.getKey().longValue();
        if (feedId > 0) {
            addFeedImgs(feedId, feed.getImgs());
            increaseTagRefCount(feed.getTagId());
        }

        return feedId;
    }

    private void addFeedImgs(long feedId, List<String> imgs) {
        try {
            String sql = "INSERT INTO SG_FeedImg(FeedId, Url, AddTime) VALUES (?, ?, NOW())";
            List<Object[]> args = new ArrayList<Object[]>();
            for (String img : imgs) {
                args.add(new Object[] { feedId, img });
            }
            jdbcTemplate.batchUpdate(sql, args);
        } catch (Exception e) {
            LOGGER.error("fail to add image for feed: {}", feedId, e);
        }
    }

    private void increaseTagRefCount(long tagId) {
        if (tagId <= 0) return;

        try {
            String sql = "UPDATE SG_FeedTag SET RefCount=RefCount+1 WHERE Id=? AND Status=1";
            update(sql, new Object[] { tagId });
        } catch (Exception e) {
            LOGGER.error("fail to increase tag ref count of tag: {}", tagId, e);
        }
    }

    @Override
    public void push(long feedId, Collection<Long> followedIds) {
        String sql = "INSERT INTO SG_FeedFollow(UserId, FeedId, AddTime) VALUES (?, ?, NOW())";
        List<Object[]> args = new ArrayList<Object[]>();
        for (long followedId : followedIds) {
            args.add(new Object[] { followedId, feedId });
        }
        jdbcTemplate.batchUpdate(sql, args);
    }

    @Override
    public Feed get(long feedId) {
        Set<Long> feedIds = Sets.newHashSet(feedId);
        List<Feed> feeds = list(feedIds);

        return feeds.isEmpty() ? Feed.NOT_EXIST_FEED : feeds.get(0);
    }

    private List<Feed> list(Collection<Long> feedIds) {
        if (feedIds.isEmpty()) return new ArrayList<Feed>();

        String sql = "SELECT Id,`Type`, UserId, Content, TagId, SubjectId, CourseId, CourseTitle, Lng, Lat, CommentCount, StarCount, AddTime FROM SG_Feed WHERE Id IN (" + StringUtils.join(feedIds, ",") + ") AND Status=1";
        List<Feed> feeds = queryList(sql, Feed.class);

        Set<Long> tagIds = new HashSet<Long>();
        Map<Long, List<String>> imgs = queryImgs(feedIds);
        Map<Long, Feed> feedsMap = new HashMap<Long, Feed>();
        for (Feed feed : feeds) {
            tagIds.add(feed.getTagId());
            feed.setImgs(imgs.get(feed.getId()));
            feedsMap.put(feed.getId(), feed);
        }

        Map<Long, String> tagNamesMap = queryTagNames(tagIds);

        List<Feed> result = new ArrayList<Feed>();
        for (long feedId : feedIds) {
            Feed feed = feedsMap.get(feedId);
            if (feed != null) result.add(feed);

            String tagName = tagNamesMap.get(feed.getTagId());
            feed.setTagName(tagName == null ? "" : tagName);
        }

        return result;
    }

    private Map<Long, List<String>> queryImgs(Collection<Long> feedIds) {
        if (feedIds.isEmpty()) return new HashMap<Long, List<String>>();

        final Map<Long, List<String>> imgs = new HashMap<Long, List<String>>();
        for (long feedId : feedIds) {
            imgs.put(feedId, new ArrayList<String>());
        }

        String sql = "SELECT FeedId, Url FROM SG_FeedImg WHERE FeedId IN (" + StringUtils.join(feedIds, ",") + ") AND Status=1";
        jdbcTemplate.query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                long feedId = rs.getLong("FeedId");
                String url = rs.getString("Url");
                imgs.get(feedId).add(url);
            }
        });

        return imgs;
    }

    private Map<Long, String> queryTagNames(Set<Long> tagIds) {
        if (tagIds.isEmpty()) return new HashMap<Long, String>();

        final Map<Long, String> tagNamesMap = new HashMap<Long, String>();
        String sql = "SELECT Id, Name FROM SG_FeedTag WHERE id IN (" + StringUtils.join(tagIds, ",") + ") AND Status=1";
        jdbcTemplate.query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                long id = rs.getLong("Id");
                String name = rs.getString("Name");
                tagNamesMap.put(id, name);
            }
        });

        return tagNamesMap;
    }

    @Override
    public boolean delete(long userId, long feedId) {
        String sql = "UPDATE SG_Feed SET Status=0 WHERE Id=? AND UserId=?";
        if (update(sql, new Object[] { feedId, userId })) {
            sql = "UPDATE SG_FeedFollow SET Status=0 WHERE FeedId=?";
            return update(sql, new Object[] { feedId });
        }

        return false;
    }

    @Override
    public List<Long> getFollowedIds(long userId) {
        String sql = "SELECT FollowedId FROM SG_UserFollow WHERE UserId=? AND Status=1";
        return queryLongList(sql, new Object[] { userId });
    }

    @Override
    public long queryFollowedCountByUser(long userId) {
        String sql = "SELECT COUNT(1) FROM SG_FeedFollow WHERE (UserId=? OR UserId=0) AND Status=1";
        return queryLong(sql, new Object[] { userId });
    }

    @Override
    public List<Feed> queryFollowedByUser(long userId, int start, int count) {
        String sql = "SELECT FeedId FROM SG_FeedFollow WHERE (UserId=? OR UserId=0) AND Status=1 ORDER BY AddTime DESC LIMIT ?,?";
        List<Long> feedIds = queryLongList(sql, new Object[] { userId, start, count });

        return list(feedIds);
    }

    @Override
    public long queryOfficialFeedsCount() {
        String sql = "SELECT COUNT(1) FROM SG_Feed WHERE Official=1 AND Status=1";
        return queryLong(sql, null);
    }

    @Override
    public List<Feed> queryOfficialFeeds(int start, int count) {
        String sql = "SELECT Id FROM SG_Feed WHERE Official=1 AND Status=1 ORDER BY AddTime DESC LIMIT ?,?";
        List<Long> feedIds = queryLongList(sql, new Object[] { start, count });

        return list(feedIds);
    }

    @Override
    public long queryCountByCourse(long courseId) {
        String sql = "SELECT COUNT(1) FROM SG_Feed WHERE CourseId=? AND Status=1";
        return queryLong(sql, new Object[] { courseId });
    }

    @Override
    public List<Feed> queryByCourse(long courseId, int start, int count) {
        String sql = "SELECT Id FROM SG_Feed WHERE CourseId=? AND Status=1 ORDER BY AddTime DESC LIMIT ?,?";
        List<Long> feedIds = queryLongList(sql, new Object[] { courseId, start, count });

        return list(feedIds);
    }

    @Override
    public void increaseCommentCount(long feedId) {
        String sql = "UPDATE SG_Feed SET CommentCount=CommentCount+1 WHERE Id=?";
        update(sql, new Object[] { feedId });
    }

    @Override
    public void decreaseCommentCount(long feedId) {
        String sql = "UPDATE SG_Feed SET CommentCount=CommentCount-1 WHERE Id=? AND CommentCount>=1";
        update(sql, new Object[] { feedId });
    }

    @Override
    public void increaseStarCount(long feedId) {
        String sql = "UPDATE SG_Feed SET StarCount=StarCount+1 WHERE Id=?";
        update(sql, new Object[] { feedId });
    }

    @Override
    public void decreaseStarCount(long feedId) {
        String sql = "UPDATE SG_Feed SET StarCount=StarCount-1 WHERE Id=? AND CommentCount>=1";
        update(sql, new Object[] { feedId });
    }

    @Override
    public long addTag(final long userId, final String tagName) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    String sql = "INSERT INTO SG_FeedTag (UserId, Name, AddTime) VALUES (?, ?, NOW())";
                    PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, userId);
                    ps.setString(2, tagName);

                    return ps;
                }
            }, keyHolder);
        } catch (Exception e) {
            throw new MomiaFailedException("添加标签失败");
        }

        return keyHolder.getKey().longValue();
    }

    @Override
    public FeedTag query(String tagName) {
        String sql = "SELECT Id, Name FROM SG_FeedTag WHERE Name=? AND Status=1";
        return queryObject(sql, new Object[] { tagName }, FeedTag.class, FeedTag.NOT_EXISTS_FEED_TAG);
    }

    @Override
    public List<FeedTag> listRecommendedTags(int count) {
        return listTags(1, count);
    }

    private List<FeedTag> listTags(int recommended, int count) {
        String sql = "SELECT Id, Name FROM SG_FeedTag WHERE Recommended=? AND Status=1 ORDER BY RefCount DESC, AddTime DESC LIMIT ?";
        return queryList(sql, new Object[] { recommended, count }, FeedTag.class);
    }


    @Override
    public List<FeedTag> listHotTags(int count) {
        return listTags(0, count);
    }
}