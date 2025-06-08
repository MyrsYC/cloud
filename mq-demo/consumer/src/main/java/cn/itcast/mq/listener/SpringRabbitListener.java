package cn.itcast.mq.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class SpringRabbitListener {

//    @RabbitListener(queues = "simple.queue")
//    public void listenerSimpleQueue(String msg){
//        System.out.println("消费者接收到simple.queue的消息:【"+msg+"】");
//    }

    @RabbitListener(queues = "simple.queue")
    public void listenerWorkQueue1(String msg) throws InterruptedException {
        System.out.println("消费者1接收到消息:【"+msg+"】"+ LocalTime.now());
        Thread.sleep(20);
    }

    @RabbitListener(queues = "simple.queue")
    public void listenerWorkQueue2(String msg) throws InterruptedException {
        System.err.println("消费者2......接收到消息:【"+msg+"】"+ LocalTime.now());
        Thread.sleep(200);
    }

    @RabbitListener(queues = "fanout.queue1")
    public void listenerFanoutQueue1(String msg){
        System.out.println("消费者接收到fanout.queue1的消息:【"+msg+"】");
    }

    @RabbitListener(queues = "fanout.queue2")
    public void listenerFanoutQueue2(String msg){
        System.out.println("消费者接收到fanout.queue2的消息:【"+msg+"】");
    }
}
