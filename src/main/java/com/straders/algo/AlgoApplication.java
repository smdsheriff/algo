package com.straders.algo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.straders.service.algobase.config.AlgoBase;

@SpringBootApplication
@ComponentScan(basePackages = { "com.straders.algo" })
@Import(AlgoBase.class)
@EnableScheduling
@EnableAsync
public class AlgoApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlgoApplication.class, args);
	}

}
