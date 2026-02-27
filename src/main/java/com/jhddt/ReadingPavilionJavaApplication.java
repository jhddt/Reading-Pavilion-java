package com.jhddt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.jhddt.module.*.mapper")
public class ReadingPavilionJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReadingPavilionJavaApplication.class, args);
    }

}
