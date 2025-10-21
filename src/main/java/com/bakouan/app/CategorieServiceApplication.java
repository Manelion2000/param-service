package com.bakouan.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.bakouan.app.repositories")
@EntityScan(basePackages = "com.bakouan.app")
@SpringBootApplication(scanBasePackages = {"com.bakouan"})
public class CategorieServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(CategorieServiceApplication.class, args);
    }

}
