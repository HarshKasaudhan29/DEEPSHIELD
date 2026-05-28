package com.detector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DetectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DetectorApplication.class, args);
    }
}
