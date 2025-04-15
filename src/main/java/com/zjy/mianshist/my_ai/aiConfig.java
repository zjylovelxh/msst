package com.zjy.mianshist.my_ai;


import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class aiConfig {

@Value("${api.apiKey}")
private String apiKey;
    @Bean
    public  ArkService aiinit(){
         ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
         Dispatcher dispatcher = new Dispatcher();
         ArkService service = ArkService.builder().timeout(Duration.ofSeconds(1800)).connectTimeout(Duration.ofSeconds(20)).dispatcher(dispatcher).connectionPool(connectionPool).baseUrl("https://ark.cn-beijing.volces.com/api/v3").apiKey(apiKey).build();
         return service;
    }
}
