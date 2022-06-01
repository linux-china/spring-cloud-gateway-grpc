package com.example.grpcserver.hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringCloudGatewayGrpcApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudGatewayGrpcApplication.class, args);
    }

    /**
     * Route locator for Gateway, priority than gPRC configuration from application.yaml
     *
     * @param rlb RouteLocatorBuilder
     * @return route locator
     */
    @Bean
    public RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb
                .routes()
                .route(routeSpec -> routeSpec
                        .path("/ip")
                        .filters(gatewayFilterSpec ->
                                gatewayFilterSpec.setHostHeader("httpbin.org")
                        )
                        .uri("https://httpbin.org/ip")
                )
                .build();
    }

}
