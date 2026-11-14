package br.com.fiap.nexusverde;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NexusVerdeApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusVerdeApplication.class, args);
    }
}
