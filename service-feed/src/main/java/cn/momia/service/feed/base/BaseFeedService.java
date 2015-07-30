package cn.momia.service.feed.base;

import cn.momia.common.service.Service;

import java.util.List;

public interface BaseFeedService extends Service {
    BaseFeed get(long id);

    long queryCountByTopic(long topicId);
    List<BaseFeed> queryByTopic(long topicId, int start, int count);
}