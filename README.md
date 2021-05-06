# gRPC_Java_QS_SSE
[protoc]

https://github.com/protocolbuffers/protobuf/releases/download/v3.15.8/protoc-3.15.8-linux-x86_64.zip

https://github.com/protocolbuffers/protobuf/releases/download/v3.15.8/protoc-3.15.8-win64.zip

https://github.com/protocolbuffers/protobuf/releases/download/v3.15.8/protoc-3.15.8-osx-x86_64.zip



[protoc-gen-grpc-java]

https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.37.0/protoc-gen-grpc-java-1.37.0-linux-x86_64.exe

https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.37.0/protoc-gen-grpc-java-1.37.0-windows-x86_64.exe

https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.37.0/protoc-gen-grpc-java-1.37.0-osx-x86_64.exe



[generate stub files]

protoc --proto_path=./ --java_out=./ --plugin=protoc-gen-grpc-java=../bin/protoc-gen-grpc-java-1.37.0-linux-x86_64.exe --grpc-java_out=./ helloworld.proto



[jar files for compiling generated stub files]

https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/3.15.8/

https://repo1.maven.org/maven2/io/grpc/grpc-api/1.37.0/

https://repo1.maven.org/maven2/io/grpc/grpc-stub/1.37.0/

https://repo1.maven.org/maven2/io/grpc/grpc-protobuf/1.37.0/

https://repo1.maven.org/maven2/com/google/guava/guava/30.1.1-jre/

export CLASSPATH=.:../libs/protobuf-java-3.15.8.jar:../libs/grpc-api-1.37.0.jar:../libs/grpc-stub-1.37.0.jar:../libs/grpc-protobuf-1.37.0.jar:../libs/guava-30.1.1-jre.jar
set CLASSPATH=.;..\libs\*


[required for JDK 9 or later]

https://repo1.maven.org/maven2/javax/annotation/javax.annotation-api/1.3.2/

export CLASSPATH=$CLASSPATH:../libs/javax.annotation-api-1.3.2.jar 

set CLASSPATH=.;..\libs\*



[javac generated stub files]

javac io/grpc/examples/helloworld/*.java

ls io/grpc/examples/helloworld/*.class



[javac server and client implementations]

javac -encoding UTF-8 server/HelloWorldServer.java

javac -encoding UTF-8 client/HelloWorldClient.java



[run Server/Client implementations]

https://repo1.maven.org/maven2/io/grpc/grpc-netty-shaded/1.37.0/

https://repo1.maven.org/maven2/io/grpc/grpc-core/1.37.0/

https://repo1.maven.org/maven2/io/grpc/grpc-context/1.37.0/

https://repo1.maven.org/maven2/io/grpc/grpc-protobuf-lite/1.37.0/

https://repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.1/

https://repo1.maven.org/maven2/io/perfmark/perfmark-api/0.23.0/

export CLASSPATH=$CLASSPATH:../libs/grpc-netty-shaded-1.37.0.jar:../libs/grpc-core-1.37.0.jar:../libs/grpc-context-1.37.0.jar:../libs/grpc-protobuf-lite-1.37.0.jar:../libs/failureaccess-1.0.1.jar:../libs/perfmark-api-0.23.0.jar
set CLASSPATH=.;..\libs\*

java server.HelloWorldServer

java client.HelloWorldClient


[javac and run SSL Server/Client with hakky54/sslcontext-kickstart]

https://repo1.maven.org/maven2/io/github/hakky54/sslcontext-kickstart/6.6.0/

https://repo1.maven.org/maven2/io/github/hakky54/sslcontext-kickstart-for-pem/6.6.0/

https://repo1.maven.org/maven2/io/github/hakky54/sslcontext-kickstart-for-netty/6.6.0/

https://repo1.maven.org/maven2/org/bouncycastle/bcprov-ext-jdk15on/1.68/

https://repo1.maven.org/maven2/org/bouncycastle/bcpkix-jdk15on/1.68/

https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.30/

https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.30/

https://repo1.maven.org/maven2/io/grpc/grpc-netty/1.37.0/

https://repo1.maven.org/maven2/org/conscrypt/conscrypt-openjdk/2.5.2/

https://repo1.maven.org/maven2/io/netty/netty-all/4.1.63.Final/

export CLASSPATH=$CLASSPATH:../libs/sslcontext-kickstart-6.6.0.jar:../libs/sslcontext-kickstart-for-pem-6.6.0.jar:../libs/sslcontext-kickstart-for-netty-6.6.0.jar:../libs/bcprov-ext-jdk15on-1.68.jar:../libs/bcpkix-jdk15on-1.68.jar:../libs/slf4j-api-1.7.30.jar:../libs/slf4j-simple-1.7.30.jar:../libs/grpc-netty-1.37.0.jar:../libs/conscrypt-openjdk-2.5.2-linux-x86_64.jar:../libs/netty-all-4.1.63.Final.jar
set CLASSPATH=.;..\libs\*

[javac and run SSE Server]

protoc --proto_path=./ --java_out=./ --plugin=protoc-gen-grpc-java=../bin/protoc-gen-grpc-java-1.37.0-linux-x86_64.exe --grpc-java_out=./ ServerSideExtension.proto

javac qlik/sse/*.java

javac -encoding UTF-8 SSE_Example/*.java

java SSE_Example.ExtensionService
