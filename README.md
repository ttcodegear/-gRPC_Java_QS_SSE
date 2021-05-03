# gRPC_Java_QS_SSE
[protoc]

https://github.com/protocolbuffers/protobuf/releases/download/v3.15.8/protoc-3.15.8-linux-x86_64.zip

https://github.com/protocolbuffers/protobuf/releases/download/v3.15.8/protoc-3.15.8-win64.zip



[protoc-gen-grpc-java]

https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.37.0/protoc-gen-grpc-java-1.37.0-linux-x86_64.exe

https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.37.0/protoc-gen-grpc-java-1.37.0-windows-x86_64.exe



[generate stub files]

protoc --proto_path=./ --java_out=./ --plugin=protoc-gen-grpc-java=../bin/protoc-gen-grpc-java-1.37.0-linux-x86_64.exe --grpc-java_out=./ helloworld.proto



[jar files for compiling generated stub files]

https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/3.15.8/

https://repo1.maven.org/maven2/io/grpc/grpc-api/1.37.0/

https://repo1.maven.org/maven2/io/grpc/grpc-stub/1.37.0/

https://repo1.maven.org/maven2/io/grpc/grpc-protobuf/1.37.0/

https://repo1.maven.org/maven2/com/google/guava/guava/30.1.1-jre/

export CLASSPATH=.:../libs/protobuf-java-3.15.8.jar:../libs/grpc-api-1.37.0.jar:../libs/grpc-stub-1.37.0.jar:../libs/grpc-protobuf-1.37.0.jar:../libs/guava-30.1.1-jre.jar



[for JDK 9+]

https://repo1.maven.org/maven2/javax/annotation/javax.annotation-api/1.3.2/

export CLASSPATH=$CLASSPATH:../libs/javax.annotation-api-1.3.2.jar 



[javac generated stub files]

javac io/grpc/examples/helloworld/*.java

ls io/grpc/examples/helloworld/*.class



[javac server and client implementations]

javac server/HelloWorldServer.java

javac client/HelloWorldClient.java



[run Server/Client implementations]

https://repo1.maven.org/maven2/io/grpc/grpc-netty-shaded/1.37.0/

https://repo1.maven.org/maven2/io/grpc/grpc-core/1.37.0/

https://repo1.maven.org/maven2/io/grpc/grpc-context/1.37.0/

https://repo1.maven.org/maven2/io/grpc/grpc-protobuf-lite/1.37.0/

https://repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.1/

https://repo1.maven.org/maven2/io/perfmark/perfmark-api/0.23.0/

export CLASSPATH=$CLASSPATH:../libs/grpc-netty-shaded-1.37.0.jar:../libs/grpc-core-1.37.0.jar:../libs/grpc-context-1.37.0.jar:../libs/grpc-protobuf-lite-1.37.0.jar:../libs/failureaccess-1.0.1.jar:../libs/perfmark-api-0.23.0.jar

java server.HelloWorldServer

java client.HelloWorldClient
