package cn.momia.service.promo.facade.impl;

import cn.momia.service.coupon.UserCouponService;
import cn.momia.service.promo.facade.PromoServiceFacade;
import cn.momia.service.coupon.Coupon;
import cn.momia.service.coupon.CouponService;
import cn.momia.service.coupon.UserCoupon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PromoServiceFacadeImpl implements PromoServiceFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(PromoServiceFacadeImpl.class);

    private CouponService couponService;
    private UserCouponService userCouponService;

    public void setCouponService(CouponService couponService) {
        this.couponService = couponService;
    }

    public void setUserCouponService(UserCouponService userCouponService) {
        this.userCouponService = userCouponService;
    }

    @Override
    public void distributeRegisterCoupon(long userId) {
        if (userId <= 0) return;

        try {
            if (userCouponService.queryCountByUserAndSrc(userId, Coupon.Src.REGISTER) > 0) return;

            List<Coupon> coupons = couponService.queryBySrc(Coupon.Src.REGISTER);
            if (coupons.isEmpty()) return;

            List<Object[]> params = new ArrayList<Object[]>();
            for (Coupon coupon : coupons) {
                for (int i = 0; i < coupon.getCount(); i++) {
                    params.add(new Object[] { userId, coupon.getId(), Coupon.Src.REGISTER, coupon.getConsumption(), coupon.getStartTime(), coupon.getEndTime() });
                }
            }

            userCouponService.add(params);
        } catch (Exception e) {
            LOGGER.error("fail to distribute user coupon to user: {}", userId, e);
        }
    }

    @Override
    public void distributeShareCoupon(long customerId, long sharerId, BigDecimal totalFee) {
        if (customerId <= 0 || sharerId <=0) return;

        int discount = calcDiscount(totalFee.intValue());
        if (discount <= 0) return;

        distributeShareCoupon(customerId, discount);
        distributeShareCoupon(sharerId, discount);
    }

    private int calcDiscount(int totalFee) {
        if (totalFee < 50) return 0;
        else if (totalFee >= 50 && totalFee < 100) return 5;
        else if (totalFee >= 100 && totalFee < 500) return 10;
        else return 50;
    }

    private void distributeShareCoupon(long userId, int discount) {
        try {
            List<Coupon> coupons = couponService.queryBySrcAndDiscount(Coupon.Src.SHARE, discount);
            if (coupons.isEmpty()) return;

            List<Object[]> params = new ArrayList<Object[]>();
            Coupon coupon = coupons.get(0);
            for (int i = 0; i < coupon.getCount(); i++) {
                params.add(new Object[] { userId, coupon.getId(), Coupon.Src.SHARE, coupon.getConsumption(), coupon.getStartTime(), coupon.getEndTime() });
            }

            userCouponService.add(params);
        } catch (Exception e) {
            LOGGER.error("fail to distribute share coupon to user: {}/{}", userId, discount, e);
        }
    }

    @Override
    public Coupon getCoupon(long userId, long orderId, long userCouponId) {
        if (userId <= 0 || orderId <= 0 || userCouponId <= 0) return Coupon.NOT_EXIST_COUPON;

        UserCoupon userCoupon = userCouponService.query(userId, orderId, userCouponId);
        if (!userCoupon.exists() || userCoupon.getCouponId() <= 0) return Coupon.NOT_EXIST_COUPON;

        return couponService.get(userCoupon.getCouponId());
    }

    @Override
    public List<Coupon> listCoupons(Collection<Integer> couponIds) {
        if (couponIds == null || couponIds.isEmpty()) return new ArrayList<Coupon>();
        return couponService.list(couponIds);
    }

    @Override
    public boolean canUse(BigDecimal totalFee, Coupon coupon) {
        return coupon.getConsumption().compareTo(totalFee) <= 0;
    }

    @Override
    public BigDecimal calcTotalFee(BigDecimal totalFee, Coupon coupon) {
        return couponService.calcTotalFee(totalFee, coupon);
    }

    @Override
    public int queryUserCouponCount(long userId, long orderId, BigDecimal totalFee, int status) {
        if (userId <= 0) return 0;
        return userCouponService.queryCountByUser(userId, orderId, totalFee, status);
    }

    @Override
    public List<UserCoupon> queryUserCoupon(long userId, long orderId, BigDecimal totalFee, int status, int start, int count) {
        if (userId <= 0 || start < 0 || count <= 0) return new ArrayList<UserCoupon>();
        return userCouponService.queryByUser(userId, orderId, totalFee, status, start, count);
    }

    @Override
    public UserCoupon getNotUsedUserCouponByOrder(long orderId) {
        if (orderId <= 0) return UserCoupon.NOT_EXIST_USER_COUPON;
        return userCouponService.queryNotUsedByOrder(orderId);
    }

    @Override
    public boolean lockUserCoupon(long userId, long orderId, long userCouponId) {
        if (userId <= 0 || orderId <= 0 || userCouponId <= 0) return true;
        return userCouponService.lock(userId, orderId, userCouponId);
    }

    @Override
    public boolean useUserCoupon(long userId, long orderId, long userCouponId) {
        if (userId <= 0 || orderId <= 0 || userCouponId <= 0) return true;
        return userCouponService.use(userId, orderId, userCouponId);
    }

    @Override
    public boolean releaseUserCoupon(long userId, long orderId) {
        if (userId <= 0 || orderId <= 0) return true;

        try {
            UserCoupon userCoupon = userCouponService.queryNotUsedByOrder(orderId);
            if (!userCoupon.exists() || userCoupon.getUserId() != userId) return true;

            if (!userCouponService.release(userId, orderId)) {
                LOGGER.error("fail to release user coupon of order: {}", orderId);
                return false;
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("fail to release user coupon of order: {}", orderId, e);
        }

        return false;
    }
}