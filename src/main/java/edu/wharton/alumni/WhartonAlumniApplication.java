package edu.wharton.alumni;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WhartonAlumniApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhartonAlumniApplication.class, args);
    }
}
