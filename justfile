services:
   grpcurl -insecure localhost:8090 list

hello:
   grpcurl -d '{"firstName": "Jackie", "lastName": "Chen"}' -insecure localhost:8090 com.example.grpcserver.hello.HelloService/hello
   