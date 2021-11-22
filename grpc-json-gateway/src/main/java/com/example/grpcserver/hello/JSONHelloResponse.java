package com.example.grpcserver.hello;

public class JSONHelloResponse {

    private String greeting;

    public JSONHelloResponse() {
    }

    public JSONHelloResponse(String greeting) {
        this.greeting = greeting;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }
}