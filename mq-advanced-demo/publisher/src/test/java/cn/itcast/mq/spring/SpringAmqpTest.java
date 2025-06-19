package cn.itcast.mq.spring;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringAmqpTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testSendMessage2SimpleQueue()  {

        //1. 准备消息
        String message = "hello, spring amqp!";
        //2 准备CorrelationData
        //2.1 消息ID
        CorrelationData correlationData=new CorrelationData(UUID.randomUUID().toString());
        //2.2 准备ConfirmCallback
        correlationData.getFuture().addCallback(result -> {
            //判断结果
            if(result.isAck()){
                //ACK
                log.debug("消息投递到交换机成功！消息ID:{}",correlationData.getId());
            }else{
                //NACK
                log.error("消息投递到交换机失败！消息ID:{}",correlationData.getId());
            }

        }, ex -> {
            // 记录日志
            log.error("消息发送失败！",ex);
            // 重发消息
        });
        //3. 发送消息
        rabbitTemplate.convertAndSend("amq.topic", "simple.test", message, correlationData);
    }

    @Test
    public void testDurableMessage(){
        //1. 准备消息
        Message message = MessageBuilder.withBody("hello, Spring ".getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        //2. 发送消息
        rabbitTemplate.convertAndSend("simple.queue",message);
    }

    @Test
    public void testTTLMessage(){
        //1. 准备消息
        Message message = MessageBuilder.
                withBody("hello, Spring ".getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setExpiration("5000")
                .build();
        //2. 发送消息
        rabbitTemplate.convertAndSend("ttl.direct","ttl",message);
        log.info("消息已经成功发送");
    }


    @Test
    public void testSendDelayMessage()  {

        //1. 准备消息
        Message message = MessageBuilder.
                withBody("hello, Spring ".getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("x-delay",5000)
                .build();
        //2 准备CorrelationData
        CorrelationData correlationData=new CorrelationData(UUID.randomUUID().toString());

        //3. 发送消息
        rabbitTemplate.convertAndSend("delay.direct", "delay", message, correlationData);
        log.info("发送消息成功");
    }


    @Test
    public void testLazyQueue()  {
        for (int i = 0; i < 100000; i++) {
            //1. 准备消息
            Message message = MessageBuilder.
                    withBody("hello, Spring ".getBytes(StandardCharsets.UTF_8))
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();

            //3. 发送消息
            rabbitTemplate.convertAndSend("lazy.queue", message);
            log.info("发送消息成功");
        }
    }

    @Test
    public void testNormaQueue()  {
        for (int i = 0; i < 100000; i++) {
            //1. 准备消息
            Message message = MessageBuilder.
                    withBody("hello, Spring ".getBytes(StandardCharsets.UTF_8))
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();

            //3. 发送消息
            rabbitTemplate.convertAndSend("norma.queue", message);
            log.info("发送消息成功");
        }

    }
}
