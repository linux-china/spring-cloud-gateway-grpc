# Adding gRPC to Spring Cloud Gateway

Starting from `3.1.0-RC1`, Spring Cloud Gateway included support for gRPC.

[gRPC](https://grpc.io/) is a high-performance Remote Procedure Call framework that can run in any environment.
It provides Bi-directional streaming, and it's based on HTTP/2.

Thanks to [reactor-netty](https://github.com/reactor/reactor-netty) and its HTTP/2 support, we were able to extend Spring Cloud Gateway to support gRPC.

## Getting started

In order to enable gRPC in Spring Cloud Gateway, since gRPC is based on `HTTP/2`, we need to enable `HTTP/2` in our project and enable SSL to enable `H2`, that can be done through configuration by adding the following:

```yaml
server:
  http2:
    enabled: true
  ssl:
    key-store-type: PKCS12
    key-store: classpath:keystore.p12
    key-store-password: password
    key-password: password
    enabled: true
```

Now that we have it enabled, we can create a route that redirects traffic to a gRPC server and take advantage of the existing filters and predicates, for example, this route will redirect traffic that comes from any path starting with `grpc` to a local server in the port `6565` and add header `X-Request-header` with the value `header-value`: 

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: grpc
          uri: https://localhost:6565
          predicates:
            - Path=/grpc/**
          filters:
           - AddResponseHeader=X-Request-header, header-value
```

## Running the samples

An end to end example can be found in this repository with a `grpc-server` that receives a `firstName` and a `lastName` that will concatenate a salutation.
A `grpc-client` that sends the message, and a `grpc-simple-gateway` that routes the requests and adds a header.

First, we need to start the server that is going to be listening to requests:

```shell
 ./gradlew :grpc-server:bootRun
```
Then, we start the gateway that is going to re-route the gRPC requests:
```shell
./gradlew :grpc-simple-gateway:bootRun
```

Finally, we can use the client that points to the gateway application:
```shell
./gradlew :grpc-client:bootRun
```

The gateway routes and filters can be modified in `grpc-simple-gateway/src/main/resources/application.yaml`

At the moment there is just one route that will forward everything to the `grpc-server`:
```yaml
      routes:
        - id: grpc
          uri: https://localhost:6565
          predicates:
            - Path=/**
          filters:
            - AddResponseHeader=X-Request-header, header-value
```

## Creating a custom Filter

Thanks to Spring Cloud Gateway flexibility, it is possible to create a custom filter to transform from a JSON payload to a gRPC message.

Even though it will have a performance impact, since we have to serialize and deserialize the requests in the gateway, it is a common pattern if you want to expose a JSON API while maintaining the internal compatibility.

For that, we can extend our `grpc-json-gateway` to include the `proto` definition with the message we want to send.

And write a custom implementation:
```java
        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            URI requestURI = exchange.getRequest().getURI();
            ManagedChannel channel = createSecuredChannel(requestURI.getHost(), 6565);

            return getDelegate().writeWith(deserializeJSONRequest()
                    .map(jsonRequest -> {
                        String firstName = jsonRequest.getFirstName();
                        String lastName = jsonRequest.getLastName();
                        return HelloServiceGrpc.newBlockingStub(channel)
                                .hello(HelloRequest.newBuilder()
                                        .setFirstName(firstName)
                                        .setLastName(lastName)
                                        .build());
                    })
                    .map(gRPCResponse -> new NettyDataBufferFactory(new PooledByteBufAllocator()).wrap(gRPCResponse.toByteArray()))
                    .cast(DataBuffer.class)
                    .last());
        }
```

The full implementation can be found in: `grpc-json-gateway/src/main/java/com/example/grpcserver/hello/JSONToGRPCFilterFactory.java`


Using the same `grpc-server`, we can start the gateway with the custom filter with:

```shell
./gradlew :grpc-json-gateway:bootRun
```

And send JSON requests to the `grpc-json-gateway` using, for example, `curl`:

```bash
curl -XPOST 'https://localhost:8082/json' -d '{"firstName":"Duff","lastName":"McKagan"}' -k -H"Content-Type: application/json"
```

We see how the gateway application forwards the requests and returns:
```bash
Hello, Duff McKagan
```

## Next Steps

In this post, we've looked at a few examples of how gRPC can be integrated within Spring Cloud Gateway. I’d love to know what other usages you've found to be helpful in your experiences.
