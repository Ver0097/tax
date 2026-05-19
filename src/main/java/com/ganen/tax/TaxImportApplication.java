package com.ganen.tax;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ganen.tax.mapper")
public class TaxImportApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxImportApplication.class, args);
    }
}