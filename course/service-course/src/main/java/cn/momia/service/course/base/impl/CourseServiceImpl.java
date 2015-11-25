package cn.momia.service.course.base.impl;

import cn.momia.api.poi.PoiServiceApi;
import cn.momia.api.poi.dto.PlaceDto;
import cn.momia.common.service.AbstractService;
import cn.momia.service.course.base.BookedCourse;
import cn.momia.service.course.base.Course;
import cn.momia.service.course.base.CourseBook;
import cn.momia.service.course.base.CourseBookImage;
import cn.momia.service.course.base.CourseComment;
import cn.momia.service.course.base.CourseDetail;
import cn.momia.service.course.base.CourseImage;
import cn.momia.service.course.base.CourseService;
import cn.momia.service.course.base.CourseSku;
import cn.momia.service.course.base.CourseSkuPlace;
import cn.momia.service.course.base.Institution;
import cn.momia.service.course.base.Teacher;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CourseServiceImpl extends AbstractService implements CourseService {
    private static final int SORT_TYPE_JOINED = 1;
    private static final int SORT_TYPE_ADDTIME = 2;

    private PoiServiceApi poiServiceApi;

    public void setPoiServiceApi(PoiServiceApi poiServiceApi) {
        this.poiServiceApi = poiServiceApi;
    }

    @Override
    public boolean isRecommended(long courseId) {
        String sql = "SELECT COUNT(1) FROM SG_CourseRecommend WHERE CourseId=? AND Status<>0";
        return queryInt(sql, new Object[] { courseId }) > 0;
    }

    @Override
    public long queryRecommendCount(long cityId) {
        String sql = "SELECT COUNT(DISTINCT A.CourseId) FROM SG_CourseRecommend A INNER JOIN SG_Course B ON A.CourseId=B.Id INNER JOIN SG_Subject C ON B.SubjectId=C.Id WHERE A.Status<>0 AND B.Status=1 AND C.Status=1 AND C.CityId=?";
        return queryLong(sql, new Object[] { cityId });
    }

    @Override
    public List<Course> queryRecomend(long cityId, int start, int count) {
        String sql = "SELECT DISTINCT A.CourseId FROM SG_CourseRecommend A INNER JOIN SG_Course B ON A.CourseId=B.Id INNER JOIN SG_Subject C ON B.SubjectId=C.Id WHERE A.Status<>0 AND B.Status=1 AND C.Status=1 AND C.CityId=? ORDER BY A.Weight DESC, A.AddTime DESC LIMIT ?,?";
        List<Long> courseIds = queryLongList(sql, new Object[] { cityId, start, count });

        return list(courseIds);
    }

    @Override
    public Course get(long courseId) {
        Collection<Long> courseIds = Sets.newHashSet(courseId);
        List<Course> courses = list(courseIds);

        return courses.isEmpty() ? Course.NOT_EXIST_COURSE : courses.get(0);
    }

    @Override
    public List<Course> list(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) return new ArrayList<Course>();

        String sql = "SELECT A.Id, A.SubjectId, A.Title, A.Cover, A.MinAge, A.MaxAge, A.Insurance, A.Joined, A.Price, A.Goal, A.Flow, A.Tips, A.Notice, A.InstitutionId, A.Status, B.Title AS Subject FROM SG_Course A INNER JOIN SG_Subject B ON A.SubjectId=B.Id WHERE A.Id IN (" + StringUtils.join(courseIds, ",") + ") AND A.Status<>0 AND B.Status<>0";
        List<Course> courses = queryObjectList(sql, Course.class);

        Set<Integer> institutionIds = new HashSet<Integer>();
        for (Course course : courses) {
            institutionIds.add(course.getInstitutionId());
        }
        Map<Integer, Institution> institutionsMap = queryInstitutions(institutionIds);
        Map<Long, List<CourseImage>> imgsMap = queryImgs(courseIds);
        Map<Long, CourseBook> booksMap = queryBooks(courseIds);
        Map<Long, List<CourseSku>> skusMap = querySkus(courseIds);
        Map<Long, BigDecimal> buyablesMap = queryBuyables(courseIds);

        for (Course course : courses) {
            Institution institution = institutionsMap.get(course.getInstitutionId());
            if (institution != null) course.setInstitution(institution.getIntro());
            course.setImgs(imgsMap.get(course.getId()));
            course.setBook(booksMap.get(course.getId()));
            course.setSkus(skusMap.get(course.getId()));

            BigDecimal price = buyablesMap.get(course.getId());
            if (price.compareTo(new BigDecimal(0)) > 0) {
                course.setPrice(price);
                course.setBuyable(true);
            }
        }

        Map<Long, Course> coursesMap = new HashMap<Long, Course>();
        for (Course course : courses) {
            coursesMap.put(course.getId(), course);
        }

        List<Course> result = new ArrayList<Course>();
        for (long courseId : courseIds) {
            Course course = coursesMap.get(courseId);
            if (course != null) result.add(course);
        }

        return result;
    }

    private Map<Integer, Institution> queryInstitutions(Collection<Integer> institutionIds) {
        if (institutionIds.isEmpty()) return new HashMap<Integer, Institution>();

        String sql = "SELECT Id, Name, Cover, Intro FROM SG_Institution WHERE Id IN (" + StringUtils.join(institutionIds, ",") + ") AND Status<>0";
        List<Institution> institutions = queryObjectList(sql, Institution.class);

        Map<Integer, Institution> institutionsMap = new HashMap<Integer, Institution>();
        for (Institution institution : institutions) {
            institutionsMap.put(institution.getId(), institution);
        }

        return institutionsMap;
    }

    private Map<Long, List<CourseImage>> queryImgs(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) return new HashMap<Long, List<CourseImage>>();

        String sql = "SELECT Id, CourseId, Url, Width, Height FROM SG_CourseImg WHERE CourseId IN (" + StringUtils.join(courseIds, ",") + ") AND Status<>0";
        List<CourseImage> imgs = queryObjectList(sql, CourseImage.class);

        final Map<Long, List<CourseImage>> imgsMap = new HashMap<Long, List<CourseImage>>();
        for (long courseId : courseIds) {
            imgsMap.put(courseId, new ArrayList<CourseImage>());
        }
        for (CourseImage img : imgs) {
            imgsMap.get(img.getCourseId()).add(img);
        }

        return imgsMap;
    }

    private Map<Long, CourseBook> queryBooks(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) return new HashMap<Long, CourseBook>();

        String sql = "SELECT Id, CourseId, Img, `Order` FROM SG_CourseBook WHERE CourseId IN (" + StringUtils.join(courseIds, ",") + ") AND Status<>0 ORDER BY `Order` ASC";
        List<CourseBookImage> imgs = queryObjectList(sql, CourseBookImage.class);

        final Map<Long, List<CourseBookImage>> imgsMap = new HashMap<Long, List<CourseBookImage>>();
        for (long courseId : courseIds) {
            imgsMap.put(courseId, new ArrayList<CourseBookImage>());
        }
        for (CourseBookImage img : imgs) {
            imgsMap.get(img.getCourseId()).add(img);
        }

        Map<Long, CourseBook> booksMap = new HashMap<Long, CourseBook>();
        for (long courseId : courseIds) {
            List<CourseBookImage> bookImgs = imgsMap.get(courseId);
            List<String> urls = new ArrayList<String>();
            for (CourseBookImage bookImg : bookImgs) {
                urls.add(bookImg.getImg());
            }

            CourseBook book = new CourseBook();
            book.setImgs(urls);

            booksMap.put(courseId, book);
        }

        return booksMap;
    }

    private Map<Long, List<CourseSku>> querySkus(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) return new HashMap<Long, List<CourseSku>>();

        String sql = "SELECT Id FROM SG_CourseSku WHERE CourseId IN (" + StringUtils.join(courseIds, ",") + ") AND Status<>0";
        List<Long> skuIds = queryLongList(sql);
        List<CourseSku> skus = listSkus(skuIds);

        Map<Long, List<CourseSku>> skusMap = new HashMap<Long, List<CourseSku>>();
        for (long courseId : courseIds) {
            skusMap.put(courseId, new ArrayList<CourseSku>());
        }
        for (CourseSku sku : skus) {
            skusMap.get(sku.getCourseId()).add(sku);
        }

        return skusMap;
    }

    private List<CourseSku> listSkus(Collection<Long> skuIds) {
        if (skuIds.isEmpty()) return new ArrayList<CourseSku>();

        String sql = "SELECT Id, CourseId, StartTime, EndTime, Deadline, Stock, UnlockedStock, LockedStock, PlaceId, Adult, Child FROM SG_CourseSku WHERE Id IN (" + StringUtils.join(skuIds, ",") + ") AND Status<>0";
        List<CourseSku> skus = queryObjectList(sql, CourseSku.class);

        Map<Long, CourseSku> skusMap = new HashMap<Long, CourseSku>();
        for (CourseSku sku : skus) {
            skusMap.put(sku.getId(), sku);
        }

        List<CourseSku> result = new ArrayList<CourseSku>();
        for (long skuId : skuIds) {
            CourseSku sku = skusMap.get(skuId);
            if (sku != null) result.add(sku);
        }

        return completeSkus(result);
    }

    private List<CourseSku> completeSkus(List<CourseSku> skus) {
        Set<Integer> placeIds = new HashSet<Integer>();
        for (CourseSku sku : skus) {
            placeIds.add(sku.getPlaceId());
        }

        List<PlaceDto> places = poiServiceApi.list(placeIds);
        Map<Integer, PlaceDto> placesMap = new HashMap<Integer, PlaceDto>();
        for (PlaceDto place : places) {
            placesMap.put(place.getId(), place);
        }

        List<CourseSku> completedSkus = new ArrayList<CourseSku>();
        for (CourseSku sku : skus) {
            PlaceDto place = placesMap.get(sku.getPlaceId());
            if (place == null) continue;

            sku.setPlace(buildCourseSkuPlace(place));
            completedSkus.add(sku);
        }

        return completedSkus;
    }

    private CourseSkuPlace buildCourseSkuPlace(PlaceDto place) {
        CourseSkuPlace courseSkuPlace = new CourseSkuPlace();
        courseSkuPlace.setId(place.getId());
        courseSkuPlace.setCityId(place.getCityId());
        courseSkuPlace.setRegionId(place.getRegionId());
        courseSkuPlace.setName(place.getName());
        courseSkuPlace.setAddress(place.getAddress());
        courseSkuPlace.setLng(place.getLng());
        courseSkuPlace.setLat(place.getLat());

        return courseSkuPlace;
    }

    private Map<Long, BigDecimal> queryBuyables(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) return new HashMap<Long, BigDecimal>();

        final Map<Long, BigDecimal> buyablesMap = new HashMap<Long, BigDecimal>();
        for (long courseId : courseIds) {
            buyablesMap.put(courseId, new BigDecimal(0));
        }

        String sql = "SELECT CourseId, Price FROM SG_SubjectSku WHERE CourseId IN (" + StringUtils.join(courseIds, ",") + ") AND CourseId>0 AND Status=1";
        query(sql, null, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                long courseId = rs.getLong("CourseId");
                BigDecimal price = rs.getBigDecimal("Price");
                buyablesMap.put(courseId, price);
            }
        });

        return buyablesMap;
    }

    @Override
    public long queryBookImgCount(long courseId) {
        String sql = "SELECT COUNT(1) FROM SG_CourseBook WHERE CourseId=? AND Status<>0";
        return queryLong(sql, new Object[] { courseId });
    }

    @Override
    public List<String> queryBookImgs(long courseId, int start, int count) {
        String sql = "SELECT Img FROM SG_CourseBook WHERE CourseId=? AND Status<>0 ORDER BY `Order` ASC LIMIT ?,?";
        return queryStringList(sql, new Object[] { courseId, start, count });
    }

    @Override
    public long queryTeacherCount(long courseId) {
        String sql = "SELECT COUNT(1) FROM SG_CourseTeacher WHERE CourseId=? AND Status<>0";
        return queryLong(sql, new Object[] { courseId });
    }

    @Override
    public List<Teacher> queryTeachers(long courseId, int start, int count) {
        String sql = "SELECT TeacherId FROM SG_CourseTeacher WHERE CourseId=? AND Status<>0 LIMIT ?,?";
        List<Integer> teacherIds = queryIntList(sql, new Object[] { courseId, start, count });

        return listTeachers(teacherIds);
    }

    private List<Teacher> listTeachers(List<Integer> teacherIds) {
        if (teacherIds.isEmpty()) return new ArrayList<Teacher>();

        String sql = "SELECT Id, Name, Avatar, Education, Experience FROM SG_Teacher WHERE Id IN (" + StringUtils.join(teacherIds, ",") + ") AND Status<>0";
        return queryObjectList(sql, Teacher.class);
    }

    @Override
    public long queryCountBySubject(long subjectId, Collection<Long> exclusions, int minAge, int maxAge) {
        String sql = exclusions.isEmpty() ?
                "SELECT COUNT(DISTINCT A.Id) FROM SG_Course A INNER JOIN SG_CourseSku B ON A.Id=B.CourseId WHERE A.SubjectId=? AND A.MinAge>=? AND A.MaxAge<=? AND A.Status=1 AND B.Deadline>NOW() AND B.Status=1" :
                "SELECT COUNT(DISTINCT A.Id) FROM SG_Course A INNER JOIN SG_CourseSku B ON A.Id=B.CourseId WHERE A.SubjectId=? AND A.MinAge>=? AND A.MaxAge<=? AND A.Id NOT IN (" + StringUtils.join(exclusions, ",") + ") AND A.Status=1 AND B.Deadline>NOW() AND B.Status=1";
        return queryLong(sql, new Object[] { subjectId, minAge, maxAge });
    }

    @Override
    public List<Course> queryBySubject(long subjectId, int start, int count, Collection<Long> exclusions, int minAge, int maxAge, int sortTypeId) {
        String sort = "MIN(B.StartTime) ASC";
        if (sortTypeId == SORT_TYPE_JOINED) {
            sort = "A.Joined DESC";
        } else if (sortTypeId == SORT_TYPE_ADDTIME) {
            sort = "A.AddTime DESC";
        }

        String sql = exclusions.isEmpty() ?
                "SELECT A.Id FROM SG_Course A INNER JOIN SG_CourseSku B ON A.Id=B.CourseId WHERE A.SubjectId=? AND A.MinAge>=? AND A.MaxAge<=? AND A.Status=1 AND B.Deadline>NOW() AND B.Status=1 GROUP BY A.Id ORDER BY " + sort + " LIMIT ?,?" :
                "SELECT A.Id FROM SG_Course A INNER JOIN SG_CourseSku B ON A.Id=B.CourseId WHERE A.SubjectId=? AND A.MinAge>=? AND A.MaxAge<=? AND A.Id NOT IN (" + StringUtils.join(exclusions, ",") + ") AND A.Status=1 AND B.Deadline>NOW() AND B.Status=1 GROUP BY A.Id ORDER BY " + sort + " LIMIT ?,?";
        List<Long> courseIds = queryLongList(sql, new Object[] { subjectId, minAge, maxAge, start, count });

        return list(courseIds);
    }

    @Override
    public List<Course> queryAllBySubject(long subjectId) {
        Set<Long> subjectIds = Sets.newHashSet(subjectId);
        Map<Long, List<Course>> coursesMap = queryAllBySubjects(subjectIds);

        return coursesMap.get(subjectId);
    }

    @Override
    public Map<Long, List<Course>>  queryAllBySubjects(Collection<Long> subjectIds) {
        if (subjectIds.isEmpty()) return new HashMap<Long, List<Course>>();

        String sql = "SELECT Id FROM SG_Course WHERE SubjectId IN (" + StringUtils.join(subjectIds, ",") + ") AND Status=1 ORDER BY AddTime DESC";
        List<Long> courseIds = queryLongList(sql);
        List<Course> courses = list(courseIds);

        Map<Long, List<Course>> coursesMap = new HashMap<Long, List<Course>>();
        for (long subjectId : subjectIds) {
            coursesMap.put(subjectId, new ArrayList<Course>());
        }
        for (Course course : courses) {
            coursesMap.get(course.getSubjectId()).add(course);
        }

        return coursesMap;
    }

    @Override
    public List<CourseSku> querySkus(long courseId, String start, String end) {
        String sql = "SELECT Id FROM SG_CourseSku WHERE CourseId=? AND StartTime>=? AND StartTime<? AND Status=1 ORDER BY StartTime ASC";
        List<Long> skuIds = queryLongList(sql, new Object[] { courseId, start, end });

        return listSkus(skuIds);
    }

    @Override
    public CourseSku getSku(long skuId) {
        Set<Long> skuIds = Sets.newHashSet(skuId);
        List<CourseSku> skus = listSkus(skuIds);

        return skus.isEmpty() ? CourseSku.NOT_EXIST_COURSE_SKU : skus.get(0);
    }

    @Override
    public boolean lockSku(long skuId) {
        String sql = "UPDATE SG_CourseSku SET UnlockedStock=UnlockedStock-1, LockedStock=LockedStock+1 WHERE Id=? AND Status=1 AND UnlockedStock>=1";
        return update(sql, new Object[] { skuId });
    }

    @Override
    public boolean unlockSku(long skuId) {
        String sql = "UPDATE SG_CourseSku SET UnlockedStock=UnlockedStock+1, LockedStock=LockedStock-1 WHERE Id=? AND Status=1 AND LockedStock>=1";
        return update(sql, new Object[] { skuId });
    }

    @Override
    public Map<Long, Date> queryStartTimesByPackages(Set<Long> packageIds) {
        if (packageIds.isEmpty()) return new HashMap<Long, Date>();

        final Map<Long, Date> startTimesMap = new HashMap<Long, Date>();
        String sql = "SELECT A.PackageId, MIN(B.StartTime) AS StartTime FROM SG_BookedCourse A INNER JOIN SG_CourseSku B ON A.CourseSkuId=B.Id WHERE A.PackageId IN (" + StringUtils.join(packageIds, ",") + ") AND A.Status<>0 AND B.Status<>0 GROUP BY A.PackageId";
        query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                startTimesMap.put(rs.getLong("PackageId"), rs.getTimestamp("StartTime"));
            }
        });

        return startTimesMap;
    }

    @Override
    public BookedCourse getBookedCourse(long bookingId) {
        Set<Long> bookingIds = Sets.newHashSet(bookingId);
        List<BookedCourse> bookedCourses = listBookedCourses(bookingIds);

        return bookedCourses.isEmpty() ? BookedCourse.NOT_EXIST_BOOKED_COURSE : bookedCourses.get(0);
    }

    private List<BookedCourse> listBookedCourses(Collection<Long> bookingIds) {
        if (bookingIds.isEmpty()) return new ArrayList<BookedCourse>();

        String sql = "SELECT A.Id, A.UserId, A.OrderId, A.PackageId, A.CourseId, A.CourseSkuId, B.StartTime, B.EndTime FROM SG_BookedCourse A INNER JOIN SG_CourseSku B ON A.CourseSkuId=B.Id WHERE A.Id IN (" + StringUtils.join(bookingIds, ",") + ") AND A.Status<>0 AND B.Status<>0";
        List<BookedCourse> bookedCourses = queryObjectList(sql, BookedCourse.class);

        Map<Long, BookedCourse> bookedCoursesMap = new HashMap<Long, BookedCourse>();
        for (BookedCourse bookedCourse : bookedCourses) {
            bookedCoursesMap.put(bookedCourse.getId(), bookedCourse);
        }

        List<BookedCourse> result = new ArrayList<BookedCourse>();
        for (long bookingId : bookingIds) {
            BookedCourse bookedCourse = bookedCoursesMap.get(bookingId);
            if (bookedCourse != null) result.add(bookedCourse);
        }

        return result;
    }

    @Override
    public long listFinishedCount() {
        String sql = "SELECT COUNT(DISTINCT A.Id) FROM SG_Course A INNER JOIN SG_CourseSku B ON A.Id=B.CourseId WHERE A.ParentId=0 AND A.Status<>0 AND B.StartTime<=NOW() AND B.Status<>0";
        return queryLong(sql);
    }

    @Override
    public List<Course> listFinished(int start, int count) {
        String sql = "SELECT A.Id FROM SG_Course A INNER JOIN SG_CourseSku B ON A.Id=B.CourseId WHERE A.ParentId=0 AND A.Status<>0 AND B.StartTime<=NOW() AND B.Status<>0 GROUP BY A.Id ORDER BY MAX(B.StartTime) DESC LIMIT ?,?";
        List<Long> courseIds =  queryLongList(sql, new Object[] { start, count });

        return list(courseIds);
    }

    @Override
    public long listFinishedCount(long userId) {
        String sql = "SELECT COUNT(DISTINCT A.CourseId) FROM SG_BookedCourse A INNER JOIN SG_CourseSku B ON A.CourseSkuId=B.Id WHERE A.UserId=? AND A.Status<>0 AND B.StartTime<=NOW() AND B.Status<>0";
        return queryLong(sql, new Object[] { userId });
    }

    @Override
    public List<Course> listFinished(long userId, int start, int count) {
        String sql = "SELECT A.CourseId FROM SG_BookedCourse A INNER JOIN SG_CourseSku B ON A.CourseSkuId=B.Id WHERE A.UserId=? AND A.Status<>0 AND B.StartTime<=NOW() AND B.Status<>0 GROUP BY A.CourseId ORDER BY MAX(B.StartTime) DESC LIMIT ?,?";
        List<Long> courseIds =  queryLongList(sql, new Object[] { userId, start, count });

        return list(courseIds);
    }

    @Override
    public long queryNotFinishedCountByUser(long userId) {
        String sql = "SELECT COUNT(1) FROM SG_BookedCourse A INNER JOIN SG_CourseSku B ON A.CourseSkuId=B.Id WHERE A.UserId=? AND A.Status<>0 AND B.StartTime>NOW() AND B.Status<>0";
        return queryLong(sql, new Object[] { userId });
    }

    @Override
    public List<BookedCourse> queryNotFinishedByUser(long userId, int start, int count) {
        String sql = "SELECT A.Id FROM SG_BookedCourse A INNER JOIN SG_CourseSku B ON A.CourseSkuId=B.Id WHERE A.UserId=? AND A.Status<>0 AND B.StartTime>NOW() AND B.Status<>0 ORDER BY B.StartTime ASC LIMIT ?,?";
        List<Long> bookingIds = queryLongList(sql, new Object[] { userId, start, count });

        return listBookedCourses(bookingIds);
    }

    @Override
    public long queryFinishedCountByUser(long userId) {
        String sql = "SELECT COUNT(1) FROM SG_BookedCourse A INNER JOIN SG_CourseSku B ON A.CourseSkuId=B.Id WHERE A.UserId=? AND A.Status<>0 AND B.StartTime<=NOW() AND B.Status<>0";
        return queryLong(sql, new Object[] { userId });
    }

    @Override
    public List<BookedCourse> queryFinishedByUser(long userId, int start, int count) {
        String sql = "SELECT A.Id FROM SG_BookedCourse A INNER JOIN SG_CourseSku B ON A.CourseSkuId=B.Id WHERE A.UserId=? AND A.Status<>0 AND B.StartTime<=NOW() AND B.Status<>0 ORDER BY B.StartTime ASC LIMIT ?,?";
        List<Long> bookingIds = queryLongList(sql, new Object[] { userId, start, count });

        return listBookedCourses(bookingIds);
    }

    @Override
    public Map<Long, Integer> queryBookedCourseCounts(Set<Long> orderIds) {
        if (orderIds.isEmpty()) return new HashMap<Long, Integer>();

        final Map<Long, Integer> map = new HashMap<Long, Integer>();
        for (long orderId : orderIds) {
            map.put(orderId, 0);
        }
        String sql = "SELECT OrderId, COUNT(1) AS Count FROM SG_BookedCourse WHERE OrderId IN (" + StringUtils.join(orderIds, ",") + ") AND Status<>0 GROUP BY OrderId";
        query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                long orderId = rs.getLong("OrderId");
                int count = rs.getInt("Count");
                map.put(orderId, count);
            }
        });

        return map;
    }

    @Override
    public Map<Long, Integer> queryFinishedCourseCounts(Set<Long> orderIds) {
        if (orderIds.isEmpty()) return new HashMap<Long, Integer>();

        final Map<Long, Integer> map = new HashMap<Long, Integer>();
        for (long orderId : orderIds) {
            map.put(orderId, 0);
        }
        String sql = "SELECT A.OrderId, COUNT(1) AS Count FROM SG_BookedCourse A INNER JOIN SG_CourseSku B ON A.CourseSkuId=B.Id WHERE A.OrderId IN (" + StringUtils.join(orderIds, ",") + ") AND A.Status<>0 AND B.EndTime<=NOW() AND B.Status<>0 GROUP BY A.OrderId";
        query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                long orderId = rs.getLong("OrderId");
                int count = rs.getInt("Count");
                map.put(orderId, count);
            }
        });

        return map;
    }

    @Override
    public List<Long> queryBookedCourseIds(long packageId) {
        if (packageId <= 0) return new ArrayList<Long>();

        String sql = "SELECT CourseId FROM SG_BookedCourse WHERE PackageId=? AND Status<>0";
        return queryLongList(sql, new Object[] { packageId });
    }

    @Override
    public boolean booked(long packageId, long courseId) {
        String sql = "SELECT COUNT(1) FROM SG_BookedCourse WHERE PackageId=? AND CourseId=? AND Status<>0";
        return queryInt(sql, new Object[] { packageId, courseId }) > 0;
    }

    @Override
    public long booking(final long userId, final long orderId, final long packageId, final CourseSku sku) {
        KeyHolder keyHolder = insert(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                String sql = "INSERT INTO SG_BookedCourse(UserId, OrderId, PackageId, CourseId, CourseSkuId, AddTime) VALUES(?, ?, ?, ?, ?, NOW())";
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, userId);
                ps.setLong(2, orderId);
                ps.setLong(3, packageId);
                ps.setLong(4, sku.getCourseId());
                ps.setLong(5, sku.getId());

                return ps;
            }
        });

        return keyHolder.getKey().longValue();
    }

    @Override
    public void increaseJoined(long courseId, int joinCount) {
        String sql = "UPDATE SG_Course SET Joined=Joined+? WHERE Id=? AND Status<>0";
        update(sql, new Object[] { joinCount, courseId });
    }

    @Override
    public boolean cancel(long userId, long bookingId) {
        String sql = "UPDATE SG_BookedCourse SET Status=0 WHERE Id=? AND UserId=? AND Status<>0";
        return update(sql, new Object[] { bookingId, userId });
    }

    @Override
    public void decreaseJoined(long courseId, int joinCount) {
        String sql = "UPDATE SG_Course SET Joined=Joined-? WHERE Id=? AND Status<>0 AND Joined>=?";
        update(sql, new Object[] { joinCount, courseId, joinCount });
    }

    @Override
    public CourseDetail getDetail(long courseId) {
        String sql = "SELECT Id, CourseId, Abstracts, Detail FROM SG_CourseDetail WHERE CourseId=? AND Status<>0";
        return queryObject(sql, new Object[] { courseId }, CourseDetail.class, CourseDetail.NOT_EXIST_COURSE_DETAIL);
    }

    @Override
    public Institution getInstitution(long courseId) {
        String sql = "SELECT B.Id, B.Name, B.Cover, B.Intro FROM SG_Course A INNER JOIN SG_Institution B ON A.InstitutionId=B.Id WHERE A.Id=? AND A.Status<>0 AND B.Status<>0";
        return queryObject(sql, new Object[] { courseId }, Institution.class, Institution.NOT_EXIST_INSTITUTION);
    }

    @Override
    public boolean matched(long subjectId, long courseId) {
        String sql = "SELECT COUNT(1) FROM SG_Course WHERE Id=? AND SubjectId=? AND Status<>0";
        return queryInt(sql, new Object[] { courseId, subjectId }) > 0;
    }

    @Override
    public boolean joined(long userId, long courseId) {
        String sql = "SELECT COUNT(1) FROM SG_BookedCourse A INNER JOIN SG_CourseSku B ON A.CourseSkuId=B.Id WHERE A.UserId=? AND A.CourseId=? AND A.Status<>0 AND B.StartTime<=NOW() AND B.Status<>0";
        return queryInt(sql, new Object[] { userId, courseId }) > 0;
    }

    @Override
    public boolean finished(long userId, long bookingId, long courseId) {
        String sql = "SELECT COUNT(1) FROM SG_BookedCourse A INNER JOIN SG_CourseSku B ON A.CourseSkuId=B.Id WHERE A.UserId=? AND A.Id=? AND A.CourseId=? AND A.Status<>0 AND B.StartTime<=NOW() AND B.Status<>0";
        return queryInt(sql, new Object[] { userId, bookingId, courseId }) > 0;
    }

    @Override
    public boolean isCommented(long userId, long bookingId) {
        String sql = "SELECT COUNT(1) FROM SG_CourseComment WHERE UserId=? AND BookingId=?";
        return queryInt(sql, new Object[] { userId, bookingId }) > 0;
    }

    @Override
    public boolean comment(CourseComment comment) {
        long commentId = addComment(comment);
        if (commentId <= 0) return false;

        if (comment.getImgs() != null && !comment.getImgs().isEmpty()) addCommentImgs(commentId, comment.getImgs());

        return true;
    }

    private long addComment(final CourseComment comment) {
        KeyHolder keyHolder = insert(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                String sql = "INSERT INTO SG_CourseComment(UserId, BookingId, CourseId, Star, Teacher, Environment, Content, AddTime) VALUES(?, ?, ?, ?, ?, ?, ?, NOW())";
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, comment.getUserId());
                ps.setLong(2, comment.getBookingId());
                ps.setLong(3, comment.getCourseId());
                ps.setInt(4, comment.getStar());
                ps.setInt(5, comment.getTeacher());
                ps.setInt(6, comment.getEnvironment());
                ps.setString(7, comment.getContent());

                return ps;
            }
        });

        return keyHolder.getKey().longValue();
    }

    private void addCommentImgs(long commentId, List<String> imgs) {
        List<Object[]> params = new ArrayList<Object[]>();
        for (String img : imgs) {
            params.add(new Object[] { commentId, img });
        }
        String sql = "INSERT INTO SG_CourseCommentImg (CommentId, Url, AddTime) VALUES (?, ?, NOW())";
        batchUpdate(sql, params);
    }

    @Override
    public long queryCommentCountByCourse(long courseId) {
        Set<Long> courseIds = Sets.newHashSet(courseId);
        return queryCommentCountByCourses(courseIds);
    }

    private long queryCommentCountByCourses(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) return 0;

        String sql = "SELECT COUNT(1) FROM SG_CourseComment WHERE CourseId IN (" + StringUtils.join(courseIds, ",") + ") AND Status<>0";
        return queryInt(sql, null);
    }

    @Override
    public List<CourseComment> queryCommentsByCourse(long courseId, int start, int count) {
        String sql = "SELECT Id FROM SG_CourseComment WHERE CourseId=? AND Status<>0 ORDER BY AddTime DESC LIMIT ?,?";
        List<Long> commentIds = queryLongList(sql, new Object[] { courseId, start, count });

        return listComments(commentIds);
    }

    private List<CourseComment> queryCommentsByCourses(Collection<Long> courseIds, int start, int count) {
        if (courseIds.isEmpty()) return new ArrayList<CourseComment>();

        String sql = "SELECT Id FROM SG_CourseComment WHERE CourseId IN (" + StringUtils.join(courseIds, ",") + ") AND Status<>0 ORDER BY AddTime DESC LIMIT ?,?";
        List<Long> commentIds = queryLongList(sql, new Object[] { start, count });

        return listComments(commentIds);
    }

    private List<CourseComment> listComments(List<Long> commentIds) {
        if (commentIds.isEmpty()) return new ArrayList<CourseComment>();

        String sql = "SELECT Id, UserId, CourseId, Star, Teacher, Environment, Content, AddTime FROM SG_CourseComment WHERE Id IN (" + StringUtils.join(commentIds, ",") + ") AND Status<>0";
        List<CourseComment> comments = queryObjectList(sql, CourseComment.class);

        Map<Long, List<String>> imgsMap = queryCommentImgs(commentIds);

        Map<Long, CourseComment> commentsMap = new HashMap<Long, CourseComment>();
        for (CourseComment comment : comments) {
            comment.setImgs(imgsMap.get(comment.getId()));
            commentsMap.put(comment.getId(), comment);
        }

        List<CourseComment> result = new ArrayList<CourseComment>();
        for (long commentId : commentIds) {
            CourseComment comment = commentsMap.get(commentId);
            if (comment != null) result.add(comment);
        }

        return result;
    }

    private Map<Long, List<String>> queryCommentImgs(List<Long> commentIds) {
        if (commentIds.isEmpty()) return new HashMap<Long, List<String>>();

        final Map<Long, List<String>> imgsMap = new HashMap<Long, List<String>>();
        for (long commentId : commentIds) {
            imgsMap.put(commentId, new ArrayList<String>());
        }

        String sql = "SELECT CommentId, Url FROM SG_CourseCommentImg WHERE CommentId IN (" + StringUtils.join(commentIds, ",") + ") AND Status<>0";
        query(sql, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                long commentId = rs.getLong("CommentId");
                String url = rs.getString("Url");
                imgsMap.get(commentId).add(url);
            }
        });

        return imgsMap;
    }

    @Override
    public long queryCommentCountBySubject(long subjectId) {
        String sql = "SELECT Id FROM SG_Course WHERE SubjectId=? AND Status<>0";
        List<Long> courseIds = queryLongList(sql, new Object[] { subjectId });

        return queryCommentCountByCourses(courseIds);
    }

    @Override
    public List<CourseComment> queryCommentsBySubject(long subjectId, int start, int count) {
        String sql = "SELECT Id FROM SG_Course WHERE SubjectId=? AND Status<>0";
        List<Long> courseIds = queryLongList(sql, new Object[] { subjectId });

        return queryCommentsByCourses(courseIds, start, count);
    }

    @Override
    public List<Long> queryCommentedBookingIds(long userId, Collection<Long> bookingIds) {
        if (userId <= 0 || bookingIds.isEmpty()) return new ArrayList<Long>();

        String sql = "SELECT BookingId FROM SG_CourseComment WHERE UserId=? AND BookingId IN (" + StringUtils.join(bookingIds, ",") + ")";
        return queryLongList(sql, new Object[] { userId });
    }
}
