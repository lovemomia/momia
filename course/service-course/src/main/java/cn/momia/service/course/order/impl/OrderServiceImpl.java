package cn.momia.service.course.order.impl;

import cn.momia.common.core.exception.MomiaErrorException;
import cn.momia.common.service.AbstractService;
import cn.momia.api.course.dto.subject.Subject;
import cn.momia.service.course.order.Order;
import cn.momia.service.course.order.OrderService;
import cn.momia.service.course.order.OrderPackage;
import cn.momia.service.course.order.Payment;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OrderServiceImpl extends AbstractService implements OrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Override
    public long add(final Order order) {
        KeyHolder keyHolder = insert(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                String sql = "INSERT INTO SG_SubjectOrder(UserId, SubjectId, Contact, Mobile, InviteCode, AddTime) VALUES(?, ?, ?, ?, ?, NOW())";
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, order.getUserId());
                ps.setLong(2, order.getSubjectId());
                ps.setString(3, order.getContact());
                ps.setString(4, order.getMobile());
                ps.setString(5, order.getInviteCode());

                return ps;
            }
        });

        long orderId = keyHolder.getKey().longValue();
        if (orderId < 0) throw new MomiaErrorException("下单失败");

        addOrderSkus(orderId, order);

        return orderId;
    }

    private void addOrderSkus(long orderId, Order order) {
        String sql = "INSERT INTO SG_SubjectOrderPackage (OrderId, SkuId, Price, CourseCount, BookableCount, Time, TimeUnit, AddTime) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        List<Object[]> args = new ArrayList<Object[]>();
        for (OrderPackage orderPackage : order.getPackages()) {
            args.add(new Object[] { orderId, orderPackage.getSkuId(), orderPackage.getPrice(), orderPackage.getBookableCount(), orderPackage.getBookableCount(), orderPackage.getTime(), orderPackage.getTimeUnit() });
        }
        batchUpdate(sql, args);
    }

    @Override
    public Order get(long orderId) {
        Set<Long> orderIds = Sets.newHashSet(orderId);
        List<Order> orders = list(orderIds);

        return orders.isEmpty() ? Order.NOT_EXIST_ORDER : orders.get(0);
    }

    @Override
    public List<Order> list(Collection<Long> orderIds) {
        if (orderIds.isEmpty()) return new ArrayList<Order>();

        String sql = "SELECT Id, UserId, SubjectId, Contact, Mobile, Status, AddTime FROM SG_SubjectOrder WHERE Id IN (" + StringUtils.join(orderIds, ",") + ") AND Status<>0";
        List<Order> orders = queryObjectList(sql, Order.class);

        Map<Long, List<OrderPackage>> packagesMap = queryOrderPackages(orderIds);
        Set<Long> skuIds = new HashSet<Long>();
        for (List<OrderPackage> packages : packagesMap.values()) {
            for (OrderPackage orderPackage : packages) {
                skuIds.add(orderPackage.getSkuId());
            }
        }

        for (Order order : orders) {
            List<OrderPackage> packages = packagesMap.get(order.getId());
            order.setPackages(packages);
        }

        Map<Long, Order> ordersMap = new HashMap<Long, Order>();
        for (Order order : orders) {
            ordersMap.put(order.getId(), order);
        }

        List<Order> result = new ArrayList<Order>();
        for (long orderId : orderIds) {
            Order order = ordersMap.get(orderId);
            if (order != null) result.add(order);
        }

        return result;
    }

    private Map<Long, List<OrderPackage>> queryOrderPackages(Collection<Long> orderIds) {
        if (orderIds.isEmpty()) return new HashMap<Long, List<OrderPackage>>();

        String sql = "SELECT Id FROM SG_SubjectOrderPackage WHERE OrderId IN (" + StringUtils.join(orderIds, ",") + ") AND Status<>0";
        List<Long> packageIds = queryLongList(sql);
        List<OrderPackage> packages = listOrderPackages(packageIds);

        Map<Long, List<OrderPackage>> packagesMap = new HashMap<Long, List<OrderPackage>>();
        for (long orderId : orderIds) {
            packagesMap.put(orderId, new ArrayList<OrderPackage>());
        }
        for (OrderPackage orderPackage : packages) {
            packagesMap.get(orderPackage.getOrderId()).add(orderPackage);
        }

        return packagesMap;
    }

    private List<OrderPackage> listOrderPackages(Collection<Long> packageIds) {
        if (packageIds.isEmpty()) return new ArrayList<OrderPackage>();

        String sql = "SELECT A.Id, A.OrderId, A.SkuId, A.Price, A.CourseCount, A.BookableCount, A.Time, A.TimeUnit, B.CourseId FROM SG_SubjectOrderPackage A INNER JOIN SG_SubjectSku B ON A.SkuId=B.Id WHERE A.Id IN (" + StringUtils.join(packageIds, ",") + ") AND A.Status<>0 AND B.Status<>0";
        List<OrderPackage> packages = queryObjectList(sql, OrderPackage.class);

        Map<Long, OrderPackage> packagesMap = new HashMap<Long, OrderPackage>();
        for (OrderPackage orderPackage : packages) {
            packagesMap.put(orderPackage.getId(), orderPackage);
        }

        List<OrderPackage> result = new ArrayList<OrderPackage>();
        for (long packageId : packageIds) {
            OrderPackage orderPackage = packagesMap.get(packageId);
            if (orderPackage != null) result.add(orderPackage);
        }

        return result;
    }

    @Override
    public boolean delete(long userId, long orderId) {
        String sql = "UPDATE SG_SubjectOrder SET Status=0 WHERE UserId=? AND Id=? AND Status<?";
        return update(sql, new Object[] { userId, orderId, Order.Status.PAYED });
    }

    @Override
    public boolean refund(long userId, long orderId) {
        String sql = "UPDATE SG_SubjectOrder SET Status=? WHERE UserId=? AND Id=? AND Status=?";
        return update(sql, new Object[] { Order.Status.TO_REFUND, userId, orderId, Order.Status.PAYED });
    }

    @Override
    public long queryCountByUser(long userId, int status) {
        if (status == 1) {
            String sql = "SELECT COUNT(1) FROM SG_SubjectOrder WHERE UserId=? AND Status>0";
            return queryLong(sql, new Object[] { userId });
        } else if (status == 2) {
            String sql = "SELECT COUNT(1) FROM SG_SubjectOrder WHERE UserId=? AND Status>0 AND Status<?";
            return queryLong(sql, new Object[] { userId, Order.Status.PAYED });
        } else if (status == 3) {
            String sql = "SELECT COUNT(1) FROM SG_SubjectOrder WHERE UserId=? AND Status>=?";
            return queryLong(sql, new Object[] { userId, Order.Status.PAYED });
        }

        return 0;
    }

    @Override
    public List<Order> queryByUser(long userId, int status, int start, int count) {
        List<Long> orderIds = new ArrayList<Long>();
        if (status == 1) {
            String sql = "SELECT Id FROM SG_SubjectOrder WHERE UserId=? AND Status>0 ORDER BY AddTime DESC LIMIT ?,?";
            orderIds = queryLongList(sql, new Object[] { userId, start, count });
        } else if (status == 2) {
            String sql = "SELECT Id FROM SG_SubjectOrder WHERE UserId=? AND Status>0 AND Status<? ORDER BY AddTime DESC LIMIT ?,?";
            orderIds = queryLongList(sql, new Object[] { userId, Order.Status.PAYED, start, count });
        } else if (status == 3) {
            String sql = "SELECT Id FROM SG_SubjectOrder WHERE UserId=? AND Status>=? ORDER BY AddTime DESC LIMIT ?,?";
            orderIds = queryLongList(sql, new Object[] { userId, Order.Status.PAYED, start, count });
        }

        return list(orderIds);
    }

    @Override
    public long queryBookableCountByUserAndOrder(long userId, long orderId) {
        String sql = "SELECT COUNT(1) FROM SG_SubjectOrder A INNER JOIN SG_SubjectOrderPackage B ON A.Id=B.OrderId WHERE A.UserId=? AND A.Id=? AND A.Status>=? AND B.Status<>0 AND B.BookableCount>0";
        return queryLong(sql, new Object[] { userId, orderId, Order.Status.PAYED });
    }

    @Override
    public List<OrderPackage> queryBookableByUserAndOrder(long userId, long orderId, int start, int count) {
        String sql = "SELECT B.Id FROM SG_SubjectOrder A INNER JOIN SG_SubjectOrderPackage B ON A.Id=B.OrderId WHERE A.UserId=? AND A.Id=? AND A.Status>=? AND B.Status<>0 AND B.BookableCount>0 ORDER BY B.AddTime ASC LIMIT ?,?";
        List<Long> packageIds = queryLongList(sql, new Object[] { userId, orderId, Order.Status.PAYED, start, count });

        return listOrderPackages(packageIds);
    }

    @Override
    public long queryBookableCountByUser(long userId) {
        String sql = "SELECT COUNT(1) FROM SG_SubjectOrder A INNER JOIN SG_SubjectOrderPackage B ON A.Id=B.OrderId WHERE A.UserId=? AND A.Status>=? AND B.Status<>0 AND B.BookableCount>0";
        return queryLong(sql, new Object[] { userId, Order.Status.PAYED });
    }

    @Override
    public List<OrderPackage> queryBookableByUser(long userId, int start, int count) {
        String sql = "SELECT B.Id FROM SG_SubjectOrder A INNER JOIN SG_SubjectOrderPackage B ON A.Id=B.OrderId WHERE A.UserId=? AND A.Status>=? AND B.Status<>0 AND B.BookableCount>0 ORDER BY B.AddTime ASC LIMIT ?,?";
        List<Long> packageIds = queryLongList(sql, new Object[] { userId, Order.Status.PAYED, start, count });

        return listOrderPackages(packageIds);
    }

    @Override
    public OrderPackage getOrderPackage(long packageId) {
        Set<Long> packageIds = Sets.newHashSet(packageId);
        List<OrderPackage> packages = listOrderPackages(packageIds);

        return packages.isEmpty() ? OrderPackage.NOT_EXIST_ORDER_PACKAGE : packages.get(0);
    }

    @Override
    public Set<Integer> getOrderPackageTypes(long orderId) {
        final Set<Integer> packageTypes = new HashSet<Integer>();
        String sql = "SELECT B.CourseId, C.Type FROM SG_SubjectOrderPackage A INNER JOIN SG_SubjectSku B ON A.SkuId=B.Id INNER JOIN SG_Subject C ON B.SubjectId=C.Id WHERE A.OrderId=? AND A.Status=1 AND B.Status<>0 AND C.Status<>0";
        query(sql, new Object[] { orderId }, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                long courseId = rs.getLong("CourseId");
                int type = rs.getInt("Type");
                if (type == Subject.Type.TRIAL) {
                    packageTypes.add(OrderPackage.Type.TRIAL);
                } else if (courseId > 0) {
                    packageTypes.add(OrderPackage.Type.SINGLE_COURSE);
                } else {
                    packageTypes.add(OrderPackage.Type.PACKAGE);
                }
            }
        });

        return packageTypes;
    }

    @Override
    public boolean prepay(long orderId) {
        String sql = "UPDATE SG_SubjectOrder SET Status=? WHERE Id=? AND (Status=? OR Status=?)";
        int updateCount = singleUpdate(sql, new Object[] { Order.Status.PRE_PAYED, orderId, Order.Status.NOT_PAYED, Order.Status.PRE_PAYED });

        return updateCount == 1;
    }

    @Override
    public boolean pay(final Payment payment) {
        try {
            execute(new TransactionCallback() {
                @Override
                public Object doInTransaction(TransactionStatus status) {
                    payOrder(payment.getOrderId());
                    logPayment(payment);

                    return null;
                }
            });
        } catch (Exception e) {
            LOGGER.error("fail to pay order: {}", payment.getOrderId(), e);
            return false;
        }

        return true;
    }

    private void payOrder(long orderId) {
        String sql = "UPDATE SG_SubjectOrder SET Status=? WHERE Id=? AND Status=?";
        int updateCount = singleUpdate(sql, new Object[] { Order.Status.PAYED, orderId, Order.Status.PRE_PAYED });

        if (updateCount != 1) throw new RuntimeException("fail to pay order: {}" + orderId);
    }

    private void logPayment(final Payment payment) {
        String sql = "INSERT INTO SG_SubjectPayment(OrderId, Payer, FinishTime, PayType, TradeNo, Fee, AddTime) VALUES(?, ?, ?, ?, ?, ?, NOW())";
        int updateCount = singleUpdate(sql, new Object[] { payment.getOrderId(), payment.getPayer(), payment.getFinishTime(), payment.getPayType(), payment.getTradeNo(), payment.getFee() });

        if (updateCount != 1) throw new RuntimeException("fail to log payment for order: " + payment.getOrderId());
    }

    @Override
    public boolean decreaseBookableCount(long packageId) {
        String sql = "UPDATE SG_SubjectOrderPackage SET BookableCount=BookableCount-1 WHERE Id=? AND Status<>0 AND BookableCount>=1";
        return update(sql, new Object[] { packageId });
    }

    @Override
    public boolean increaseBookableCount(long packageId) {
        String sql = "UPDATE SG_SubjectOrderPackage SET BookableCount=BookableCount+1 WHERE Id=? AND Status<>0 AND BookableCount<CourseCount";
        return update(sql, new Object[] { packageId });
    }

    @Override
    public boolean hasTrialOrder(long userId) {
        String sql = "SELECT COUNT(1) FROM SG_SubjectOrder A INNER JOIN SG_Subject B ON A.SubjectId=B.Id WHERE A.UserId=? AND A.Status<>0 AND B.`Type`=?";
        return queryInt(sql, new Object[] { userId, Subject.Type.TRIAL }) > 0;
    }

    @Override
    public int getBoughtCount(long userId, long skuId) {
        String sql = "SELECT COUNT(1) FROM SG_SubjectOrder A INNER JOIN SG_SubjectOrderPackage B ON A.Id=B.OrderId WHERE A.UserId=? AND B.SkuId=? AND A.Status<>0 AND B.Status<>0";
        return queryInt(sql, new Object[] { userId, skuId });
    }
}
