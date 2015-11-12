package cn.momia.service.course.subject.coupon.impl;

import cn.momia.common.api.exception.MomiaFailedException;
import cn.momia.common.service.DbAccessService;
import cn.momia.service.course.subject.coupon.Coupon;
import cn.momia.service.course.subject.coupon.CouponService;
import cn.momia.service.course.subject.coupon.UserCoupon;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CouponServiceImpl extends DbAccessService implements CouponService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CouponServiceImpl.class);

    private static final int COUPON_SRC_INVITE = 1;

    private static final int NOT_USED_STATUS = 1;
    private static final int USED_STATUS = 2;
    private static final int EXPIRED_STATUS = 3;

    @Override
    public UserCoupon get(long userCouponId) {
        Set<Long> userCouponIds = Sets.newHashSet(userCouponId);
        List<UserCoupon> userCoupons = list(userCouponIds);

        return userCoupons.isEmpty() ? UserCoupon.NOT_EXIST_USER_COUPON : userCoupons.get(0);
    }

    private List<UserCoupon> list(Collection<Long> userCouponIds) {
        if (userCouponIds.isEmpty()) return new ArrayList<UserCoupon>();

        String sql = "SELECT A.Id, B.Type, A.UserId, A.CouponId, B.Title, B.Desc, B.Discount, B.Consumption, A.StartTime, A.EndTime, A.Status FROM SG_UserCoupon A INNER JOIN SG_Coupon B ON A.CouponId=B.Id WHERE A.Id IN (" + StringUtils.join(userCouponIds, ",") + ") AND A.Status<>0 AND B.Status=1";
        List<UserCoupon> userCoupons = queryList(sql, UserCoupon.class);

        Map<Long, UserCoupon> userCouponsMap = new HashMap<Long, UserCoupon>();
        for (UserCoupon userCoupon : userCoupons) {
            userCouponsMap.put(userCoupon.getId(), userCoupon);
        }

        List<UserCoupon> result = new ArrayList<UserCoupon>();
        for (long userCouponId : userCouponIds) {
            UserCoupon userCoupon = userCouponsMap.get(userCouponId);
            if (userCoupon != null) result.add(userCoupon);
        }

        return result;
    }

    @Override
    public long queryCount(long userId, int status) {
        String sql;
        if (status == NOT_USED_STATUS) {
            sql = "SELECT COUNT(1) FROM SG_UserCoupon A INNER JOIN SG_Coupon B ON A.CouponId=B.Id WHERE A.UserId=? AND A.Status=1 AND A.EndTime>NOW() AND B.Status=1";
        } else if (status == USED_STATUS) {
            sql = "SELECT COUNT(1) FROM SG_UserCoupon A INNER JOIN SG_Coupon B ON A.CouponId=B.Id WHERE A.UserId=? AND A.Status=2 AND B.Status=1";
        } else if (status == EXPIRED_STATUS) {
            sql = "SELECT COUNT(1) FROM SG_UserCoupon A INNER JOIN SG_Coupon B ON A.CouponId=B.Id WHERE A.UserId=? AND A.Status=1 AND A.EndTime<=NOW() AND B.Status=1";
        } else {
            sql = "SELECT COUNT(1) FROM SG_UserCoupon A INNER JOIN SG_Coupon B ON A.CouponId=B.Id WHERE A.UserId=? AND (A.Status=2 OR (A.Status=1 AND A.EndTime>NOW())) AND B.Status=1";
        }

        return queryLong(sql, new Object[] { userId });
    }

    @Override
    public List<UserCoupon> query(long userId, int status, int start, int count) {
        String sql;
        if (status == NOT_USED_STATUS) {
            sql = "SELECT A.Id FROM SG_UserCoupon A INNER JOIN SG_Coupon B ON A.CouponId=B.Id WHERE A.UserId=? AND A.Status=1 AND A.EndTime>NOW() AND B.Status=1 ORDER BY A.EndTime ASC, A.StartTime ASC LIMIT ?,?";
        } else if (status == USED_STATUS) {
            sql = "SELECT A.Id FROM SG_UserCoupon A INNER JOIN SG_Coupon B ON A.CouponId=B.Id WHERE A.UserId=? AND A.Status=2 AND B.Status=1 ORDER BY A.EndTime ASC, A.StartTime ASC LIMIT ?,?";
        } else if (status == EXPIRED_STATUS) {
            sql = "SELECT A.Id FROM SG_UserCoupon A INNER JOIN SG_Coupon B ON A.CouponId=B.Id WHERE A.UserId=? AND A.Status=1 AND A.EndTime<=NOW() AND B.Status=1 ORDER BY A.EndTime ASC, A.StartTime ASC LIMIT ?,?";
        } else {
            sql = "SELECT A.Id FROM SG_UserCoupon A INNER JOIN SG_Coupon B ON A.CouponId=B.Id WHERE A.UserId=? AND (A.Status=2 OR (A.Status=1 AND A.EndTime>NOW())) AND B.Status=1 ORDER BY A.EndTime ASC, A.StartTime ASC LIMIT ?,?";
        }
        List<Long> userCouponIds = queryLongList(sql, new Object[] { userId, start, count });

        return list(userCouponIds);
    }

    @Override
    public UserCoupon queryByOrder(long orderId) {
        String sql = "SELECT UserCouponId FROM SG_SubjectOrderCoupon WHERE OrderId=? AND Status=1";
        List<Long> userCouponIds = queryLongList(sql, new Object[] { orderId });
        List<UserCoupon> userCoupons = list(userCouponIds);

        return userCoupons.isEmpty() ? UserCoupon.NOT_EXIST_USER_COUPON : userCoupons.get(0);
    }

    @Override
    public BigDecimal calcTotalFee(BigDecimal totalFee, UserCoupon userCoupon) {
        return totalFee.compareTo(userCoupon.getDiscount()) > 0 ? totalFee.subtract(userCoupon.getDiscount()) : new BigDecimal(0);
    }

    @Override
    public boolean preUseCoupon(long orderId, long userCouponId) {
        long orderCouponId = getOrderCouponId(orderId);
        if (orderCouponId <= 0) {
            String sql = "INSERT INTO SG_SubjectOrderCoupon (OrderId, UserCouponId, AddTime) VALUES (?, ?, NOW())";
            return update(sql, new Object[] { orderId, userCouponId });
        } else {
            String sql = "UPDATE SG_SubjectOrderCoupon SET UserCouponId=?, Status=1 WHERE OrderId=?";
            return update(sql, new Object[] { userCouponId, orderId });
        }
    }

    private long getOrderCouponId(long orderId) {
        String sql = "SELECT Id FROM SG_SubjectOrderCoupon WHERE OrderId=?";
        return queryLong(sql, new Object[] { orderId });
    }

    @Override
    public boolean useCoupon(long orderId, long userCouponId) {
        String sql = "UPDATE SG_UserCoupon SET OrderId=?, Status=2 WHERE Id=? AND (OrderId=0 OR OrderId=?)";
        return update(sql, new Object[] { orderId, userCouponId, orderId });
    }

    @Override
    public boolean hasInviteCoupon(String mobile) {
        String sql = "SELECT COUNT(1) FROM SG_InviteCoupon WHERE Mobile=? AND Status<>0";
        return queryInt(sql, new Object[] { mobile }) > 0;
    }

    @Override
    public boolean addInviteCoupon(String mobile, String inviteCode) {
        String sql = "SELECT Id FROM SG_Coupon WHERE Src=? AND OnlineTime<=NOW() AND OfflineTime>NOW() AND Status=1 ORDER BY AddTime DESC LIMIT 1";
        List<Integer> couponIds = queryIntList(sql, new Object[] { COUPON_SRC_INVITE });

        List<Coupon> coupons = listCoupons(couponIds);
        if (!coupons.isEmpty()) return addInviteCoupon(mobile, inviteCode, coupons.get(0));

        throw new MomiaFailedException("不能再领取了，活动已经结束了哦");
    }

    private List<Coupon> listCoupons(Collection<Integer> couponIds) {
        if (couponIds.isEmpty()) return new ArrayList<Coupon>();

        String sql = "SELECT Id, Count, TimeType, Time, TimeUnit, StartTime, EndTime FROM SG_Coupon WHERE Id IN(" + StringUtils.join(couponIds, ",") + ") AND Status=1";
        List<Coupon> coupons = queryList(sql, Coupon.class);

        Map<Integer, Coupon> couponsMap = new HashMap<Integer, Coupon>();
        for (Coupon coupon : coupons) {
            couponsMap.put(coupon.getId(), coupon);
        }

        List<Coupon> result = new ArrayList<Coupon>();
        for (int couponId : couponIds) {
            Coupon coupon = couponsMap.get(couponId);
            if (coupon != null) result.add(coupon);
        }

        return result;
    }

    private boolean addInviteCoupon(String mobile, String inviteCode, Coupon coupon) {
        String sql = "INSERT INTO SG_InviteCoupon (Mobile, InviteCode, CouponId, AddTime) VALUES (?, ?, ?, NOW())";
        try {
            return update(sql, new Object[] { mobile, inviteCode, coupon.getId() });
        } catch (Exception e) {
            LOGGER.error("fail to add invite coupon for mobile: {}", mobile, e);
            return false;
        }
    }

    @Override
    public void addInviteUserCoupon(long userId, String mobile) {
        int couponId = getInviteCouponId(mobile);
        if (couponId <= 0) return;

        List<Coupon> coupons = listCoupons(Sets.newHashSet(couponId));
        if (coupons.isEmpty()) return;

        List<Object[]> args = new ArrayList<Object[]>();
        for (Coupon coupon : coupons) {
            for (int i = 0; i < coupon.getCount(); i++) {
                int timeType = coupon.getTimeType();
                Date startTime;
                Date endTime;
                if (timeType == 1) {
                    startTime = new Date();
                    int time = coupon.getTime();
                    int timeUnit = coupon.getTimeUnit();
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(startTime);
                    switch (timeUnit) {
                        case Coupon.TimeUnit.YEAR:
                            calendar.add(Calendar.YEAR, time);
                            break;
                        case Coupon.TimeUnit.QUATER:
                            calendar.add(Calendar.MONTH, time * 3);
                            break;
                        default:
                            calendar.add(Calendar.MONTH, time);
                    }
                    endTime = calendar.getTime();
                } else {
                    startTime = coupon.getStartTime();
                    endTime = coupon.getEndTime();
                }

                args.add(new Object[] { userId, coupon.getId(), startTime, endTime });
            }
        }

        String sql = "INSERT INTO SG_UserCoupon (UserId, CouponId, StartTime, EndTime, AddTime) VALUES (?, ?, ?, ?, NOW())";
        jdbcTemplate.batchUpdate(sql, args);
    }

    private int getInviteCouponId(String mobile) {
        String sql = "SELECT CouponId FROM SG_InviteCoupon WHERE Mobile=? AND Status=1";
        return queryInt(sql, new Object[] { mobile });
    }
}
