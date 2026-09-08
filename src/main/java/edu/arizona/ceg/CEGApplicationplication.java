package edu.arizona.ceg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CEGApplicationplication {

    public static void main(String[] args) {

        SpringApplication.run(CEGApplicationplication.class, args);
    }

}