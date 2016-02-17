package cn.momia.service.feed.star;

import java.util.Collection;
import java.util.List;

public interface FeedStarService {
    boolean isStared(long userId, long feedId);
    List<Long> filterNotStaredFeedIds(long userId, Collection<Long> feedIds);

    boolean add(long userId, long feedId);
    boolean delete(long userId, long feedId);

    long queryUserIdsCount(long feedId);
    List<Long> queryUserIds(long feedId, int start, int count);
}
