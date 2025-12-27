package com.immortals.platform.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@EnableFeignClients
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.immortals.platform.order",
    "com.immortals.platform.common"
})
public class OrderApplication {

	/**
	 * Entry point of the OrderService application.
	 * 
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}

}
