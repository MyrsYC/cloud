package com.heima.item.config;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.item.pojo.Item;
import com.heima.item.pojo.ItemStock;
import com.heima.item.service.IItemService;
import com.heima.item.service.IItemStockService;
import com.heima.item.service.impl.ItemStockService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisHandler implements InitializingBean {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IItemStockService stockService;

    @Autowired
    private IItemService itemService;

    private static final ObjectMapper MAPPER = new ObjectMapper();


    //大概在Bean属性赋值之后执行
    @Override
    public void afterPropertiesSet() throws Exception {
        //初始化缓存

        //1.查询商品信息
        List<Item> itemList = itemService.list();
        //2.放入缓存
        for (Item item : itemList) {
            //2.1 item序列化为JSON
            String json = MAPPER.writeValueAsString(item);
            //2.2存入redis
            redisTemplate.opsForValue().set("item:id:" + item.getId(), json);
        }

        //3.查询商品库存信息
        List<ItemStock> stockList = stockService.list();
        //4.放入缓存
        for (ItemStock itemStock : stockList) {
            //4.1 item序列化为JSON
            String json = MAPPER.writeValueAsString(itemStock);
            //4.2存入redis
            redisTemplate.opsForValue().set("item:stock:id:" + itemStock.getId(), json);
        }
    }

    public void saveItem(Item item) {
        try {
            String json = MAPPER.writeValueAsString(item);
            redisTemplate.opsForValue().set("item:id:" + item.getId(), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteItemById(Long id) {
        redisTemplate.delete("item:id:" + id);
    }

}
