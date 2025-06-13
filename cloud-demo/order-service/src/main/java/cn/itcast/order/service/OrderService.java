package cn.itcast.order.service;


import cn.itcast.feign.clients.UserClient;
import cn.itcast.feign.pojo.User;
import cn.itcast.order.mapper.OrderMapper;
import cn.itcast.order.pojo.Order;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Resource
    private UserClient userClient;

    public Order queryOrderById(Long orderId) {
        // 1.查询订单
        Order order = orderMapper.findById(orderId);
        // 2.利用Feign发起Http请求，查询用户
        User user=userClient.findById(order.getUserId());
        // 3. 封装user到Order
        order.setUser(user);
        // 4.返回
        return order;
    }

    @SentinelResource("goods")
    public void queryGoods(){
        System.err.println("查询商品");
    }
    /*@Autowired
    private RestTemplate restTemplate;
    public Order queryOrderById(Long orderId) {
        // 1.查询订单
        Order order = orderMapper.findById(orderId);
        // 2. 利用RestTemplate,发起请求
        String url="http://userservice/user/"+order.getUserId();
        User user=restTemplate.getForObject(url, User.class);
        // 3. 封装user到Order
        order.setUser(user);
        // 4.返回
        return order;
    }*/
}
