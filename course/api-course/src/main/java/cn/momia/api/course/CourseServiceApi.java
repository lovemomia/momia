package cn.momia.api.course;

import cn.momia.api.course.dto.BookedCourse;
import cn.momia.api.course.dto.Course;
import cn.momia.api.course.dto.CourseSku;
import cn.momia.api.course.dto.TeacherCourse;
import cn.momia.api.course.dto.TimelineUnit;
import cn.momia.api.course.dto.UserCourseComment;
import cn.momia.api.course.dto.CourseDetail;
import cn.momia.api.course.dto.DatedCourseSkus;
import cn.momia.api.course.dto.Favorite;
import cn.momia.api.course.dto.Institution;
import cn.momia.api.course.dto.Teacher;
import cn.momia.common.core.HttpServiceApi;
import cn.momia.common.core.dto.PagedList;
import cn.momia.common.core.http.MomiaHttpParamBuilder;
import cn.momia.common.core.http.MomiaHttpRequestBuilder;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpUriRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CourseServiceApi extends HttpServiceApi {
    public PagedList<Course> listRecommend(int cityId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("city", cityId)
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/recommend"), builder.build());

        return executeReturnPagedList(request, Course.class);
    }

    public PagedList<Course> listTrial(int cityId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("city", cityId)
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/trial"), builder.build());

        return executeReturnPagedList(request, Course.class);
    }

    public Course get(long courseId) {
        return get(courseId, Course.ShowType.FULL);
    }

    public Course get(long courseId, int type) {
        return get(courseId, "", type);
    }

    public Course get(long courseId, String pos) {
        return get(courseId, pos, Course.ShowType.FULL);
    }

    public Course get(long courseId, String pos, int type) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("pos", pos)
                .add("type", type);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/%d", courseId), builder.build());

        return executeReturnObject(request, Course.class);
    }

    public PagedList<Course> listFinished(long userId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("uid", userId)
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/finished/list"), builder.build());

        return executeReturnPagedList(request, Course.class);
    }

    public PagedList<Course> query(long subjectId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("suid", subjectId)
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/query"), builder.build());

        return executeReturnPagedList(request, Course.class);
    }

    public PagedList<Course> query(long subjectId, long packageId, int minAge, int maxAge, int sortTypeId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("suid", subjectId)
                .add("pid", packageId)
                .add("min", minAge)
                .add("max", maxAge)
                .add("sort", sortTypeId)
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/query"), builder.build());

        return executeReturnPagedList(request, Course.class);
    }

    public CourseDetail detail(long courseId) {
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/%d/detail", courseId));
        return executeReturnObject(request, CourseDetail.class);
    }

    public PagedList<String> book(long courseId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/%d/book", courseId), builder.build());

        return executeReturnPagedList(request, String.class);
    }

    public PagedList<Teacher> teacher(long courseId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/%d/teacher", courseId), builder.build());

        return executeReturnPagedList(request, Teacher.class);
    }

    public Institution institution(long courseId) {
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/%d/institution", courseId));
        return executeReturnObject(request, Institution.class);
    }

    public Map<Long, String> queryTips(Set<Long> courseIds) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("coids", StringUtils.join(courseIds, ","));
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/tips"), builder.build());
        return executeReturnObject(request, Map.class);
    }

    public CourseSku getSku(long courseId, long skuId) {
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/%d/sku/%d", courseId, skuId));
        return executeReturnObject(request, CourseSku.class);
    }

    public List<DatedCourseSkus> listWeekSkus(long courseId) {
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/%d/sku/week", courseId));
        return executeReturnList(request, DatedCourseSkus.class);
    }

    public List<DatedCourseSkus> listMonthSkus(long courseId, int month) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("month", month);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/%d/sku/month", courseId), builder.build());

        return executeReturnList(request, DatedCourseSkus.class);
    }

    public PagedList<BookedCourse> queryNotFinishedByUser(long userId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("uid", userId)
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/notfinished"), builder.build());

        return executeReturnPagedList(request, BookedCourse.class);
    }

    public PagedList<BookedCourse> queryFinishedByUser(long userId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("uid", userId)
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/finished"), builder.build());

        return executeReturnPagedList(request, BookedCourse.class);
    }

    public TeacherCourse getOngoingTeacherCourse(long userId) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("uid", userId);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/teacher/ongoing"), builder.build());

        return executeReturnObject(request, TeacherCourse.class);
    }

    public PagedList<TeacherCourse> queryNotFinishedByTeacher(long userId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("uid", userId)
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/teacher/notfinished"), builder.build());

        return executeReturnPagedList(request, TeacherCourse.class);
    }

    public PagedList<TeacherCourse> queryFinishedByTeacher(long userId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("uid", userId)
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/teacher/finished"), builder.build());

        return executeReturnPagedList(request, TeacherCourse.class);
    }

    public boolean joined(long userId, long courseId) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("uid", userId);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/%d/joined", courseId), builder.build());

        return executeReturnObject(request, Boolean.class);
    }

    public BookedCourse booking(String utoken, long packageId, long skuId) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("utoken", utoken)
                .add("pid", packageId)
                .add("sid", skuId);
        HttpUriRequest request = MomiaHttpRequestBuilder.POST(url("/course/booking"), builder.build());

        return executeReturnObject(request, BookedCourse.class);
    }

    public BookedCourse cancel(String utoken, long bookingId) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("utoken", utoken)
                .add("bid", bookingId);
        HttpUriRequest request = MomiaHttpRequestBuilder.POST(url("/course/cancel"), builder.build());

        return executeReturnObject(request, BookedCourse.class);
    }

    public boolean comment(JSONObject commentJson) {
        HttpUriRequest request = MomiaHttpRequestBuilder.POST(url("/course/comment"), commentJson.toString());
        return executeReturnObject(request, Boolean.class);
    }

    public PagedList<UserCourseComment> queryCommentsByCourse(long courseId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/%d/comment", courseId), builder.build());

        return executeReturnPagedList(request, UserCourseComment.class);
    }

    public List<String> getLatestImgs(long userId) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("uid", userId);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/comment/img"), builder.build());

        return executeReturnList(request, String.class);
    }

    public boolean isFavored(long userId, long courseId) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("uid", userId);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/%d/favored", courseId), builder.build());

        return executeReturnObject(request, Boolean.class);
    }

    public boolean favor(long userId, long courseId) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("uid", userId);
        HttpUriRequest request = MomiaHttpRequestBuilder.POST(url("/course/%d/favor", courseId), builder.build());

        return executeReturnObject(request, Boolean.class);
    }

    public boolean unfavor(long userId, long courseId) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder().add("uid", userId);
        HttpUriRequest request = MomiaHttpRequestBuilder.POST(url("/course/%d/unfavor", courseId), builder.build());

        return executeReturnObject(request, Boolean.class);
    }

    public PagedList<Favorite> listFavorites(long userId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("uid", userId)
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/favorite"), builder.build());

        return executeReturnPagedList(request, Favorite.class);
    }

    public PagedList<TimelineUnit> timelineOfUser(long userId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("uid", userId)
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/course/timeline"), builder.build());

        return executeReturnPagedList(request, TimelineUnit.class);
    }

    public PagedList<TimelineUnit> commentTimelineOfUser(long userId, int start, int count) {
        MomiaHttpParamBuilder builder = new MomiaHttpParamBuilder()
                .add("uid", userId)
                .add("start", start)
                .add("count", count);
        HttpUriRequest request = MomiaHttpRequestBuilder.GET(url("/comment/timeline"), builder.build());

        return executeReturnPagedList(request, TimelineUnit.class);
    }
}
