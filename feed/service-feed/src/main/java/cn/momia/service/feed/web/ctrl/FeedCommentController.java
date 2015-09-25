package cn.momia.service.feed.web.ctrl;

import cn.momia.api.feed.dto.FeedCommentDto;
import cn.momia.api.user.UserServiceApi;
import cn.momia.api.user.dto.UserDto;
import cn.momia.common.api.dto.PagedList;
import cn.momia.common.api.http.MomiaHttpResponse;
import cn.momia.common.webapp.ctrl.BaseController;
import cn.momia.service.feed.comment.FeedComment;
import cn.momia.service.feed.facade.Feed;
import cn.momia.service.feed.facade.FeedServiceFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/feed")
public class FeedCommentController extends BaseController {
    @Autowired private FeedServiceFacade feedServiceFacade;

    @RequestMapping(value = "/{id}/comment/list", method = RequestMethod.GET)
    public MomiaHttpResponse listComments(@PathVariable long id, @RequestParam int start, @RequestParam int count) {
        if (isInvalidLimit(start, count)) return MomiaHttpResponse.SUCCESS(PagedList.EMPTY);

        Feed feed = feedServiceFacade.getFeed(id);
        if (!feed.exists()) return MomiaHttpResponse.FAILED("无效的Feed");

        long totalCount = feedServiceFacade.queryCommentsCount(id);
        if (totalCount <= 0) return MomiaHttpResponse.SUCCESS(PagedList.EMPTY);

        List<FeedComment> comments = feedServiceFacade.queryComments(id, start, count);

        List<Long> userIds = new ArrayList<Long>();
        for (FeedComment comment : comments) userIds.add(comment.getUserId());
        List<UserDto> users = UserServiceApi.USER.list(userIds, UserDto.Type.MINI);
        Map<Long, UserDto> usersMap = new HashMap<Long, UserDto>();
        for (UserDto user : users) usersMap.put(user.getId(), user);

        PagedList<FeedCommentDto> pagedFeedCommentDtos = new PagedList(totalCount, start, count);
        List<FeedCommentDto> feedCommentDtos = new ArrayList<FeedCommentDto>();
        for (FeedComment comment : comments) {
            UserDto user = usersMap.get(comment.getUserId());
            if (user == null) continue;

            feedCommentDtos.add(buildFeedCommentDto(comment, user));
        }
        pagedFeedCommentDtos.setList(feedCommentDtos);

        return MomiaHttpResponse.SUCCESS(pagedFeedCommentDtos);
    }

    private FeedCommentDto buildFeedCommentDto(FeedComment comment, UserDto user) {
        FeedCommentDto feedCommentDto = new FeedCommentDto();
        feedCommentDto.setId(comment.getId());
        feedCommentDto.setContent(comment.getContent());
        feedCommentDto.setAddTime(comment.getAddTime());
        feedCommentDto.setNickName(user.getNickName());
        feedCommentDto.setAvatar(user.getAvatar());

        return feedCommentDto;
    }

    @RequestMapping(value = "/{id}/comment", method = RequestMethod.POST)
    public MomiaHttpResponse addComment(@RequestParam(value = "uid") long userId, @PathVariable long id, @RequestParam String content) {
        Feed feed = feedServiceFacade.getFeed(id);
        if (!feed.exists()) return MomiaHttpResponse.FAILED("无效的Feed");

        if (!feedServiceFacade.addComment(userId, id, content)) return MomiaHttpResponse.FAILED("发表评论失败");

        feedServiceFacade.increaseCommentCount(id);
        return MomiaHttpResponse.SUCCESS;
    }

    @RequestMapping(value = "/{id}/comment/{cmid}", method = RequestMethod.DELETE)
    public MomiaHttpResponse deleteComment(@RequestParam(value = "uid") long userId, @PathVariable long id, @PathVariable(value = "cmid") long commentId) {
        if (!feedServiceFacade.deleteComment(userId, id, commentId)) return MomiaHttpResponse.FAILED("删除评论失败");

        feedServiceFacade.decreaseCommentCount(id);
        return MomiaHttpResponse.SUCCESS;
    }
}