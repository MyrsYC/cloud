package cn.itcast.mq.spring;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
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
}
