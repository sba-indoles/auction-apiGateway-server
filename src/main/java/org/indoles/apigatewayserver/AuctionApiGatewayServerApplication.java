package org.indoles.apigatewayserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AuctionApiGatewayServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionApiGatewayServerApplication.class, args);
    }

}
