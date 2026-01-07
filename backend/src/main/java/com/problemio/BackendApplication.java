package com.problemio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    //DB 연결 확인
//    @Bean
//    public CommandLineRunner testEnvConnection(@Value("${DB_URL}") String dbUrl) {
//        return args -> {
//            System.out.println("====================================================");
//            System.out.println(" ENV 파일 연결 확인 완료!");
//            System.out.println(" 가져온 DB URL: " + dbUrl);
//            System.out.println("====================================================");
//        };
    }

