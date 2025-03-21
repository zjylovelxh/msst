package com.zjy.mianshist.config;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
 
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
public class RedissonConfigure {
    @Bean
    public RedissonClient redissonRxClient(){
 
        //创建配置
        Config config = new Config();
        //设置redis地址和使用的几号datebase
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setDatabase(1);
        //创建实例  支持同步异步
        RedissonClient redisson = Redisson.create(config);
        //返回redisson
        return redisson;
    }
}