package com.immortals.platform.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentApplicationTests {

	@Test
	void contextLoads() {
		// This test will fail if the application context cannot start
		// or if there are any issues with the configuration.
		// You can add more specific tests here to check the functionality of your application.
		// For example, you can test the payment processing logic, database interactions, etc.
		// For now, we will just check if the context loads successfully.
	}

}
