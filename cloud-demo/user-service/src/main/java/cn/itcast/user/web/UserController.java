package cn.itcast.user.web;

import cn.itcast.user.config.PatternProperties;
import cn.itcast.user.pojo.User;
import cn.itcast.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/user")
//@RefreshScope
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PatternProperties properties;

//    @Value("${pattern.dateformat}")
//    private String dateformat;

    @GetMapping("prop")
    public PatternProperties properties(){
        return properties;
    }
    /**
     * 检验读取nacos中的配置
     * @return 日期
     */
    @GetMapping("now")
    public String now(){
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(properties.getDateformat()));
    }


        /**
         * 路径： /user/110
         *
         * @param id 用户id
         * @return 用户
         */
    @GetMapping("/{id}")
    public User queryById(@PathVariable("id") Long id,@RequestHeader(value= "Truth", required =false) String truth) throws InterruptedException {
        if(id==1){
            //休眠，触发熔断
            Thread.sleep(60);
        } else if (id==2) {
            //抛出异常，触发熔断
            throw new RuntimeException("故意出错，触发异常");
        }
        return userService.queryById(id);
    }
}
