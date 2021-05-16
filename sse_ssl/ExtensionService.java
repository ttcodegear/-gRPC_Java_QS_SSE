package SSE_Example_ssl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.Metadata;
import io.grpc.Status;

import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.SslContext;
import nl.altindag.ssl.util.PemUtils;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.NettySslUtils;

import qlik.sse.*;

class ExtensionServiceImpl extends ConnectorGrpc.ConnectorImplBase {
  private ThreadLocal<Metadata> tl_headers = new ThreadLocal<Metadata>();

  public void setMetadata(Metadata headers) {
    tl_headers.set(headers);
  }

  private int getFunctionId() {
    int func_id = -1;
    try {
      byte[] data = tl_headers.get().get(Metadata.Key.of("qlik-functionrequestheader-bin",
                                                         Metadata.BINARY_BYTE_MARSHALLER));
      func_id = ServerSideExtension.FunctionRequestHeader.parseFrom(data).getFunctionId();
    }
    catch (Exception ex) {}
    return func_id;
  }

  @Override
  public StreamObserver<ServerSideExtension.BundledRows> executeFunction(
                                        StreamObserver<ServerSideExtension.BundledRows> responseObserver) {
    System.out.println("executeFunction");
    StreamObserver<ServerSideExtension.BundledRows> sumOfColumn =
                                                    new StreamObserver<ServerSideExtension.BundledRows>() {
      List<Double> params = new ArrayList<Double>();
      @Override
      public void onNext(ServerSideExtension.BundledRows bundledRows) {
        for(ServerSideExtension.Row row : bundledRows.getRowsList()) {
          params.add(row.getDuals(0).getNumData()); // row=[Col1]
        }
      }

      @Override
      public void onError(Throwable t) {
        t.printStackTrace();
      }

      @Override
      public void onCompleted() {
        double result = params.stream().mapToDouble(d -> d).sum(); // Col1 + Col1 + ...
        ServerSideExtension.Dual dual = ServerSideExtension.Dual.newBuilder().setNumData(result).build();
        ServerSideExtension.Row row = ServerSideExtension.Row.newBuilder().addDuals(dual).build();
        ServerSideExtension.BundledRows.Builder builder = ServerSideExtension.BundledRows.newBuilder();
        ServerSideExtension.BundledRows reply = builder.addRows(row).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
      }
    };

    StreamObserver<ServerSideExtension.BundledRows> sumOfRows =
                                                    new StreamObserver<ServerSideExtension.BundledRows>() {
      @Override
      public void onNext(ServerSideExtension.BundledRows bundledRows) {
        ServerSideExtension.BundledRows.Builder response_rows = ServerSideExtension.BundledRows.newBuilder();
        for(ServerSideExtension.Row r : bundledRows.getRowsList()) {
          List<ServerSideExtension.Dual> duals = r.getDualsList();
          double param1 = duals.get(0).getNumData(); // row=[Col1,Col2]
          double param2 = duals.get(1).getNumData();
          double result = param1 + param2;           // Col1 + Col2
          ServerSideExtension.Dual dual = ServerSideExtension.Dual.newBuilder().setNumData(result).build();
          ServerSideExtension.Row row = ServerSideExtension.Row.newBuilder().addDuals(dual).build();
          response_rows.addRows(row);
        }
        responseObserver.onNext(response_rows.build());
      }

      @Override
      public void onError(Throwable t) {
        t.printStackTrace();
      }

      @Override
      public void onCompleted() {
        responseObserver.onCompleted();
      }
    };

    int func_id = getFunctionId();
    if(func_id == 0) {
      System.out.println("sumOfColumn");
      return sumOfColumn;
    }
    else if(func_id == 1) {
      System.out.println("sumOfRows");
      return sumOfRows;
    }
    else {
      Status status = Status.UNIMPLEMENTED.withDescription("Method not implemented!");
      responseObserver.onError(status.asRuntimeException());
      return responseObserver;
    }
  }

  @Override
  public void getCapabilities(ServerSideExtension.Empty request,
                              StreamObserver<ServerSideExtension.Capabilities> responseObserver) {
    System.out.println("getCapabilities");
    ServerSideExtension.Capabilities capabilities = ServerSideExtension.Capabilities.newBuilder()
      .setAllowScript(false)
      .setPluginIdentifier("Simple SSE Test")
      .setPluginVersion("v0.0.1")
      .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
        .setFunctionId(0)                                              // 関数ID
        .setName("SumOfColumn")                                        // 関数名
        .setFunctionType(ServerSideExtension.FunctionType.AGGREGATION) // 関数タイプ=0=スカラー,1=集計,2=テンソル
        .setReturnType(ServerSideExtension.DataType.NUMERIC)           // 関数戻り値=0=文字列,1=数値,2=Dual
        .addParams(ServerSideExtension.Parameter.newBuilder()
          .setName("col1")                                             // パラメータ名
          .setDataType(ServerSideExtension.DataType.NUMERIC)))         // パラメータタイプ=0=文字列,1=数値,2=Dual
      .addFunctions(ServerSideExtension.FunctionDefinition.newBuilder()
        .setFunctionId(1)                                              // 関数ID
        .setName("SumOfRows")                                          // 関数名
        .setFunctionType(ServerSideExtension.FunctionType.TENSOR)      // 関数タイプ=0=スカラー,1=集計,2=テンソル
        .setReturnType(ServerSideExtension.DataType.NUMERIC)           // 関数戻り値=0=文字列,1=数値,2=Dual
        .addParams(ServerSideExtension.Parameter.newBuilder()
          .setName("col1")                                             // パラメータ名
          .setDataType(ServerSideExtension.DataType.NUMERIC))          // パラメータタイプ=0=文字列,1=数値,2=Dual
        .addParams(ServerSideExtension.Parameter.newBuilder()
          .setName("col2")                                             // パラメータ名
          .setDataType(ServerSideExtension.DataType.NUMERIC)))         // パラメータタイプ=0=文字列,1=数値,2=Dual
      .build();

    responseObserver.onNext(capabilities);
    responseObserver.onCompleted();
  }
}

public class ExtensionService {
  private Server server;

  private void start() throws IOException {
    X509ExtendedKeyManager keyManager = PemUtils.loadIdentityMaterial("SSE_Example_ssl/sse_server_cert.pem", "SSE_Example_ssl/sse_server_key.pem");
    X509ExtendedTrustManager trustManager = PemUtils.loadTrustMaterial("SSE_Example_ssl/root_cert.pem");
    SSLFactory sslFactory = SSLFactory.builder().withIdentityMaterial(keyManager).withTrustMaterial(trustManager).build();
    SslContext sslContext = GrpcSslContexts.configure(NettySslUtils.forServer(sslFactory)).build();

    ExtensionServiceImpl service = new ExtensionServiceImpl();
    server = NettyServerBuilder.forPort(50053).addService(service).intercept(
                                              new SSEInterceptor(service)).sslContext(sslContext).build().start();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          ExtensionService.this.server.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
          ex.printStackTrace();
        }
      }
    });
  }

  public static void main(String[] args) throws Exception {
    ExtensionService sse_server = new ExtensionService();
    sse_server.start();
    // Await termination on the main thread since the grpc library uses daemon threads.
    sse_server.server.awaitTermination();
  }
}
