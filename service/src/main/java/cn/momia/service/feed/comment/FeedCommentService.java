package cn.momia.service.feed.comment;

import cn.momia.common.service.Service;

import java.util.List;

public interface FeedCommentService extends Service {
    int queryCount(long feedId);
    List<FeedComment> query(long feedId, int start, int count);
}
