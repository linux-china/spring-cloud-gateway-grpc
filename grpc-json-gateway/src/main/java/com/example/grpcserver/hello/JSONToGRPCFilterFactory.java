package com.example.grpcserver.hello;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.cert.X509Certificate;

import static io.grpc.netty.shaded.io.grpc.netty.NegotiationType.TLS;

@Component
public class JSONToGRPCFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory;

    public JSONToGRPCFilterFactory(ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory) {
        this.modifyResponseBodyGatewayFilterFactory = modifyResponseBodyGatewayFilterFactory;
    }

    @Override
    public GatewayFilter apply(Object config) {
        GatewayFilter filter = new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                ModifyResponseBodyGatewayFilterFactory.Config modifyResponseBodyConfig = new ModifyResponseBodyGatewayFilterFactory.Config();
                modifyResponseBodyConfig.setInClass(JSONHelloRequest.class);
                modifyResponseBodyConfig.setOutClass(JSONHelloResponse.class);
                modifyResponseBodyConfig.setNewContentType("application/json");

                RewriteFunction<JSONHelloRequest, JSONHelloResponse> rewriteFunction = (rewriteExchange, body) -> {
                    URI requestURI = rewriteExchange.getRequest().getURI();
                    ManagedChannel channel = createSecuredChannel(requestURI.getHost(), 6565);
                    String firstName = body.getFirstName();
                    String lastName = body.getLastName();
                    HelloResponse greetingFromGRPC = HelloServiceGrpc.newBlockingStub(channel)
                            .hello(HelloRequest.newBuilder()
                                    .setFirstName(firstName)
                                    .setLastName(lastName)
                                    .build());

                    return Mono.just(new JSONHelloResponse(greetingFromGRPC.getGreeting()));
                };
                modifyResponseBodyConfig.setRewriteFunction(rewriteFunction);

                return Mono.just(modifyResponseBodyGatewayFilterFactory.apply(modifyResponseBodyConfig))
                                .then(chain.filter(exchange));
            }
        };
        int order = NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER + 1;
        return new OrderedGatewayFilter(filter, order);
    }

    @Override
    public String name() {
        return "JSONToGRPCFilter";
    }

    private ManagedChannel createSecuredChannel(String host, int port) {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certs,
                                                   String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs,
                                                   String authType) {
                    }
                }};

        try {
            return NettyChannelBuilder.forAddress(host, port)
                    .useTransportSecurity().sslContext(
                            GrpcSslContexts.forClient().trustManager(trustAllCerts[0])
                                    .build()).negotiationType(TLS).build();
        } catch (SSLException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }
}