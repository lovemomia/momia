package cn.momia.service.deal.order;

import cn.momia.common.service.Service;

import java.util.List;

public interface OrderService extends Service {
    long add(Order order);
    Order get(long id);
    long queryCountByUser(long userId, int status);
    List<Order> queryByUser(long userId, int status, int start, int count);
    List<Order> queryByUserAndSku(long userId, long skuId);
    List<Order> queryAllCustomerOrderByProduct(long productId);
    List<Order> queryDistinctCustomerOrderByProduct(long productId, int start, int count);
    boolean delete(long userId, long id);
    boolean prepay(long id);
    boolean pay(long id);
    boolean check(long userId, long id, long productId, long skuId);
}
