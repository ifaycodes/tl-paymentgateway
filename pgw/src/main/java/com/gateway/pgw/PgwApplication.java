package com.gateway.pgw;

import java.util.Objects;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.gateway.carddetails.CardDetail;
import com.gateway.gateway.GatewayApis;

@SpringBootApplication
public class PgwApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(PgwApplication.class, args);
		/* int amount = 1000;
		String customerId = "CUS001";
		String orderId = "cus001-odr001";
		CardDetail cardDetail = new CardDetail();
		cardDetail.setCardNumber("4111111111111111");
		cardDetail.setCvv("123");
		cardDetail.setExpiryMonth(12);
		cardDetail.setExpiryYear(2030);


		GatewayApis gatewayApis = new GatewayApis();

		gatewayApis.authorize(amount, orderId, customerId, cardDetail);
		*/

	}




























	public int hash() {
		final String name = "Ifeoma";
		int code = Objects.hash(name);
		System.out.println(code);
		
		return code;
	}

}
