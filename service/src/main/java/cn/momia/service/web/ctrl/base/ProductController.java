package cn.momia.service.web.ctrl.base;

import cn.momia.common.web.response.ResponseMessage;
import cn.momia.service.base.product.Customer;
import cn.momia.service.base.product.PlayMates;
import cn.momia.service.base.product.Product;
import cn.momia.service.base.product.ProductQuery;
import cn.momia.service.base.product.ProductService;
import cn.momia.service.base.product.sku.Sku;
import cn.momia.service.base.product.sku.SkuProperty;
import cn.momia.service.base.user.User;
import cn.momia.service.base.user.UserService;
import cn.momia.service.base.user.participant.Participant;
import cn.momia.service.base.user.participant.ParticipantService;
import cn.momia.service.deal.order.Order;
import cn.momia.service.deal.order.OrderService;
import cn.momia.service.web.ctrl.AbstractController;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
@RequestMapping("/product")
public class ProductController extends AbstractController {
    @Autowired private ProductService productService;

    @Autowired private OrderService orderService;
    @Autowired private UserService userService;
    @Autowired private ParticipantService participantService;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseMessage getProducts(@RequestParam(value = "city") int cityId,
                                       @RequestParam int start,
                                       @RequestParam int count,
                                       @RequestParam(required = false) String query) {
        if (cityId < 0 || isInvalidLimit(start, count)) return ResponseMessage.BAD_REQUEST;

        long totalCount = productService.queryCount(new ProductQuery(cityId, query));
        List<Product> products = productService.query(start, count, new ProductQuery(cityId, query));

        JSONObject productsPackJson = new JSONObject();
        productsPackJson.put("totalCount", totalCount);
        productsPackJson.put("products", products);

        return new ResponseMessage(productsPackJson);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseMessage getProduct(@PathVariable long id) {
        if (id <= 0) return ResponseMessage.BAD_REQUEST;

        Product product = productService.get(id);
        if (!product.exists()) return ResponseMessage.BAD_REQUEST;

        return new ResponseMessage(product);
    }

    @RequestMapping(value = "/{id}/sku", method = RequestMethod.GET)
    public ResponseMessage getProductSkus(@PathVariable long id) {
        if (id <= 0) return ResponseMessage.BAD_REQUEST;

        List<Sku> skus = productService.getSkus(id);

        return new ResponseMessage(skus);
    }

    @RequestMapping(value = "/{id}/customer", method = RequestMethod.GET)
    public ResponseMessage getProductCustomersInfo(@PathVariable long id, @RequestParam int start, @RequestParam int count) {
        if (id <= 0 || isInvalidLimit(start, count)) return ResponseMessage.BAD_REQUEST;

        List<Order> orders = orderService.queryDistinctCustomerOrderByProduct(id, start, count);
        List<Customer> customers = new ArrayList<Customer>();
        Map<Long, List<Long>> skuCustomerIdsMap = new HashMap<Long, List<Long>>();
        List<Long> participantIds = new ArrayList<Long>();
        List<Sku> skus = productService.getSkus(id);
        List<SkuProperty> skuProperties = new ArrayList<SkuProperty>();
        Map<Long, List<Long>> participantsMap = new HashMap<Long, List<Long>>();
        Map<List<SkuProperty>, List<Customer>> validCustomersMap = new HashMap<List<SkuProperty>, List<Customer>>();
        List<PlayMates> playMates = new ArrayList<PlayMates>();
        for(Sku sku : skus) {
            List<Long> customerIds = new ArrayList<Long>();
            for (Order order : orders) {
                if (sku.getId() == order.getSkuId()) {
                    Customer customer = new Customer();
                    customer.setUserId(order.getCustomerId());
                    customer.setOrderDate(order.getAddTime());
                    customer.setOrderStatus(order.getStatus());
                    customers.add(customer);
                    customerIds.add(order.getCustomerId());
                    participantIds.addAll(order.getParticipants());
                    participantsMap.put(order.getCustomerId(), order.getParticipants());
                }
            }
            skuCustomerIdsMap.put(sku.getId(), customerIds);


            Map<Long, User> users = userService.get(skuCustomerIdsMap.get(sku.getId()));
            Map<Long, Participant> participants = participantService.get(participantIds);


            List<Customer> validCustomers = new ArrayList<Customer>();
            for (int i = 0; i < customers.size(); i++) {
                Customer customer = customers.get(i);
                User user = users.get(customer.getUserId());
                if (user == null || !user.exists()) continue;
                customer.setAvatar(user.getAvatar());
                customer.setName(user.getName());
                customer.setChildren(user.getChildren());
                customer.setUserId(user.getId());
                customer.setNickName(user.getNickName());

                List<Participant> participantList = new ArrayList<Participant>();
                for (long participantId : participantsMap.get(customer.getUserId())) {
                    participantList.add(participants.get(participantId));
                }
                customer.setParticipants(participantList);
                validCustomers.add(customer);
            }
            validCustomersMap.put(sku.getProperties(), validCustomers);

            PlayMates playMate = new PlayMates();
            playMate.setSkuProperties(sku.getProperties());
            playMate.setCustomers(validCustomers);
            playMates.add(playMate);

            System.out.println(sku.getProperties()+":"+validCustomers );
        }




        return new ResponseMessage(playMates);
    }
}
