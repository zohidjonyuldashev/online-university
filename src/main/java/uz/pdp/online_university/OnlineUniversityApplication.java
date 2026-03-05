package uz.pdp.online_university;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class OnlineUniversityApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineUniversityApplication.class, args);
    }

}
