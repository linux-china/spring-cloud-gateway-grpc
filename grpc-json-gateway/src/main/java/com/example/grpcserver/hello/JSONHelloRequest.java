package com.example.grpcserver.hello;

public class JSONHelloRequest {

    private String firstName;
    private String lastName;

    public JSONHelloRequest() {
    }

    public JSONHelloRequest(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}