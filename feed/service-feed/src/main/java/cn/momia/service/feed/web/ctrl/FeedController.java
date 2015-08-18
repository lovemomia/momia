package cn.momia.service.feed.web.ctrl;

import cn.momia.service.feed.comment.FeedComment;
import cn.momia.service.feed.facade.Feed;
import cn.momia.service.feed.facade.FeedServiceFacade;
import cn.momia.service.feed.web.ctrl.dto.FeedCommentDto;
import cn.momia.service.feed.web.ctrl.dto.FeedDto;
import cn.momia.api.user.UserServiceApi;
import cn.momia.api.user.User;
import cn.momia.service.base.web.ctrl.AbstractController;
import cn.momia.service.base.web.ctrl.dto.PagedListDto;
import cn.momia.service.base.web.response.ResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/feed")
public class FeedController extends AbstractController {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedController.class);

    @Autowired private FeedServiceFacade feedServiceFacade;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseMessage list(@RequestParam(value = "uid") long userId, @RequestParam int start, @RequestParam int count) {
        if (isInvalidLimit(start, count)) return ResponseMessage.SUCCESS(PagedListDto.EMPTY);

        long totalCount = feedServiceFacade.queryFollowedCountByUser(userId);
        List<Feed> feeds = feedServiceFacade.queryFollowedByUser(userId, start, count);

        return ResponseMessage.SUCCESS(buildFeedsDto(userId, feeds, totalCount, start, count));
    }

    private PagedListDto buildFeedsDto(long userId, List<Feed> feeds, long totalCount, @RequestParam int start, @RequestParam int count) {
        Set<Long> staredFeedIds = new HashSet<Long>();
        if (userId > 0) {
            Set<Long> feedIds = new HashSet<Long>();
            for (Feed feed : feeds) feedIds.add(feed.getId());
            staredFeedIds.addAll(feedServiceFacade.queryStaredFeeds(userId, feedIds));
        }

        Set<Long> userIds = new HashSet<Long>();
        for (Feed feed : feeds) userIds.add(feed.getUserId());
        List<User> users = UserServiceApi.USER.list(userIds, User.Type.MINI);
        Map<Long, User> usersMap = new HashMap<Long, User>();
        for (User user : users) usersMap.put(user.getId(), user);

        PagedListDto feedsDto = new PagedListDto(totalCount, start, count);
        for (Feed feed : feeds) {
            User user = usersMap.get(feed.getUserId());
            if (user == null) continue;

            feedsDto.add(new FeedDto(feed, user, staredFeedIds.contains(feed.getId())));
        }
        return feedsDto;
    }

    @RequestMapping(value = "/topic", method = RequestMethod.GET)
    public ResponseMessage topic(@RequestParam(value = "uid") long userId,
                                 @RequestParam(value = "tid") long topicId,
                                 @RequestParam int start,
                                 @RequestParam int count) {
        if (topicId <= 0 || isInvalidLimit(start, count)) return ResponseMessage.SUCCESS(PagedListDto.EMPTY);

        long totalCount = feedServiceFacade.queryCountByTopic(topicId);
        List<Feed> feeds = feedServiceFacade.queryByTopic(topicId, start, count);

        return ResponseMessage.SUCCESS(buildFeedsDto(userId, feeds, totalCount, start, count));
    }

    @RequestMapping(method = RequestMethod.POST, consumes = "application/json")
    public ResponseMessage add(@RequestBody Feed feed) {
        long feedId = feedServiceFacade.addFeed(feed);
        if (feedId <= 0) return ResponseMessage.FAILED("发表Feed失败");
        try {
            // TODO 异步推送
            List<Long> followedIds = feedServiceFacade.getFollowedIds(feed.getUserId());
            followedIds.add(feed.getUserId());
            feedServiceFacade.pushFeed(feedId, followedIds);
        } catch (Exception e) {
            LOGGER.error("fail to push feed: {}", feed.getId());
        }

        return ResponseMessage.SUCCESS;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseMessage get(@RequestParam(value = "uid", defaultValue = "0") long userId, @PathVariable long id) {
        Feed feed = feedServiceFacade.getFeed(id);
        if (!feed.exists()) return ResponseMessage.FAILED("无效的Feed");

        User feedUser = UserServiceApi.USER.get(feed.getUserId());
        if (feedUser.getId() <= 0) return ResponseMessage.FAILED("无效的Feed");

        boolean stared = feedServiceFacade.isStared(userId, id);

        return ResponseMessage.SUCCESS(new FeedDto(feed, feedUser, stared));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseMessage delete(@RequestParam(value = "uid") long userId, @PathVariable long id) {
        if (!feedServiceFacade.deleteFeed(userId, id)) return ResponseMessage.FAILED("删除Feed失败");
        return ResponseMessage.SUCCESS;
    }

    @RequestMapping(value = "/{id}/comment/list", method = RequestMethod.GET)
    public ResponseMessage listComments(@PathVariable long id, @RequestParam int start, @RequestParam int count) {
        if (isInvalidLimit(start, count)) return ResponseMessage.SUCCESS(PagedListDto.EMPTY);

        Feed feed = feedServiceFacade.getFeed(id);
        if (!feed.exists()) return ResponseMessage.FAILED("无效的Feed");

        long totalCount = feedServiceFacade.queryCommentsCount(id);
        if (totalCount <= 0) return ResponseMessage.SUCCESS(PagedListDto.EMPTY);

        List<FeedComment> comments = feedServiceFacade.queryComments(id, start, count);

        List<Long> userIds = new ArrayList<Long>();
        for (FeedComment comment : comments) userIds.add(comment.getUserId());
        List<User> users = UserServiceApi.USER.list(userIds, User.Type.MINI);
        Map<Long, User> usersMap = new HashMap<Long, User>();
        for (User user : users) usersMap.put(user.getId(), user);

        PagedListDto feedCommentsDto = new PagedListDto(totalCount, start, count);
        for (FeedComment comment : comments) {
            User user = usersMap.get(comment.getUserId());
            if (user == null) continue;

            feedCommentsDto.add(new FeedCommentDto(comment, user));
        }

        return ResponseMessage.SUCCESS(feedCommentsDto);
    }

    @RequestMapping(value = "/{id}/comment", method = RequestMethod.POST)
    public ResponseMessage addComment(@RequestParam(value = "uid") long userId, @PathVariable long id, @RequestParam String content) {
        Feed feed = feedServiceFacade.getFeed(id);
        if (!feed.exists()) return ResponseMessage.FAILED("无效的Feed");

        if (!feedServiceFacade.addComment(userId, id, content)) return ResponseMessage.FAILED("发表评论失败");

        feedServiceFacade.increaseCommentCount(id);
        return ResponseMessage.SUCCESS;
    }

    @RequestMapping(value = "/{id}/comment/{cmid}", method = RequestMethod.DELETE)
    public ResponseMessage deleteComment(@RequestParam(value = "uid") long userId, @PathVariable long id, @PathVariable(value = "cmid") long commentId) {
        if (!feedServiceFacade.deleteComment(userId, id, commentId)) return ResponseMessage.FAILED("删除评论失败");

        feedServiceFacade.decreaseCommentCount(id);
        return ResponseMessage.SUCCESS;
    }

    @RequestMapping(value = "/{id}/star/list", method = RequestMethod.GET)
    public ResponseMessage listStaredUsers(@PathVariable long id, @RequestParam int start, @RequestParam int count) {
        if (isInvalidLimit(start, count)) return ResponseMessage.SUCCESS(PagedListDto.EMPTY);

        Feed feed = feedServiceFacade.getFeed(id);
        if (!feed.exists()) return ResponseMessage.FAILED("无效的Feed");

        long totalCount = feedServiceFacade.queryStaredUsersCount(id);
        if (totalCount <= 0) return ResponseMessage.SUCCESS(PagedListDto.EMPTY);

        List<Long> userIds = feedServiceFacade.queryStaredUserIds(id, start, count);
        List<User> users = UserServiceApi.USER.list(userIds, User.Type.MINI);

        PagedListDto staredUsersDto = new PagedListDto(totalCount, start, count);
        for (User user : users) staredUsersDto.add(user);

        return ResponseMessage.SUCCESS(staredUsersDto);
    }

    @RequestMapping(value = "/{id}/star", method = RequestMethod.POST)
    public ResponseMessage star(@RequestParam(value = "uid") long userId, @PathVariable long id) {
        Feed feed = feedServiceFacade.getFeed(id);
        if (!feed.exists()) return ResponseMessage.FAILED("无效的Feed");

        if (!feedServiceFacade.star(userId, id)) return ResponseMessage.FAILED("赞失败");

        feedServiceFacade.increaseStarCount(id);
        return ResponseMessage.SUCCESS;
    }

    @RequestMapping(value = "/{id}/unstar", method = RequestMethod.POST)
    public ResponseMessage unstar(@RequestParam(value = "uid") long userId, @PathVariable long id) {
        if (!feedServiceFacade.unstar(userId, id)) return ResponseMessage.FAILED("取消赞失败");

        feedServiceFacade.decreaseStarCount(id);
        return ResponseMessage.SUCCESS;
    }
}
