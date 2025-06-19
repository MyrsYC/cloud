package cn.itcast.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CommonConfig implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        //获取RabbitTemplate对象
        RabbitTemplate rabbitTemplate =applicationContext.getBean(RabbitTemplate.class);
        //配置ReturnCallback
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            //判断是否是延迟消息
            if (message.getMessageProperties().getReceivedDelay()>0) {
                //是延迟消息，忽略错误提示
                return;
            }

            //记录日志
            log.error("消息发送到队列失败,响应码: {},失败原因:{},交换机:{},路由key:{},消息:{}", replyCode, replyText, exchange, routingKey,message);
            //有需要可以实现重发消息
        });
    }
}
