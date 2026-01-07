package com.problemio.global.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.problemio.**.mapper")
public class MyBatisConfig {
}
