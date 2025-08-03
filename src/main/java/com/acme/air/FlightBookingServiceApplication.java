package com.acme.air;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@SpringBootApplication
public class FlightBookingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlightBookingServiceApplication.class, args);
	}

}
