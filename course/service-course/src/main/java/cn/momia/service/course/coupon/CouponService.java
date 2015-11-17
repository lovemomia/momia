package cn.momia.service.course.coupon;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    UserCoupon get(long userCouponId);
    BigDecimal calcTotalFee(BigDecimal totalFee, UserCoupon userCoupon);

    long queryCount(long userId, int status);
    List<UserCoupon> query(long userId, int status, int start, int count);

    UserCoupon queryByOrder(long orderId);

    boolean preUseCoupon(long orderId, long userCouponId);
    boolean useCoupon(long orderId, long userCouponId);

    boolean hasInviteCoupon(String mobile);
    boolean addInviteCoupon(String mobile, String inviteCode);

    InviteCoupon getInviteCoupon(String mobile);
    boolean updateInviteCouponStatus(String mobile);
    void distributeInviteUserCoupon(long userId, int couponId);
}