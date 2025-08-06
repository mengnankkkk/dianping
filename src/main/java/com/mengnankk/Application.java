package com.mengnankk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;

@SpringBootApplication(exclude = {
    ElasticsearchDataAutoConfiguration.class,
    RabbitAutoConfiguration.class
})
@MapperScan("com.mengnankk.mapper")
public class Application {
    public static void main(String[] args) {
        // 暂时禁用Spring AI自动配置来排查问题
        System.setProperty("spring.ai.autoconfigure.enabled", "false");
        SpringApplication.run(Application.class, args);
    }
}

