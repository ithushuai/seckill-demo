package com.hushuai.seckill.config;

import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * created by it_hushuai
 * 2020/5/4 16:59
 */
@Configuration
public class RedissonConfiguration {
    @Bean
    public Redisson redisson(){
        //单机模式
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setDatabase(0);
        return (Redisson) Redisson.create(config);
    }
}
