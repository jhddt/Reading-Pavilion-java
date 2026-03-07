package com.jhddt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.jhddt.module.*.mapper")
@EnableAsync
public class ReadingPavilionJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReadingPavilionJavaApplication.class, args);
    }

}
