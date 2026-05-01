package com.example.tictac.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class GameSessionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GameSessionServiceApplication.class, args);
	}
}
