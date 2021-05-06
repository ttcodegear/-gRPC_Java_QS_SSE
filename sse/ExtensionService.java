package SSE_Example;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.Metadata;
import io.grpc.Status;

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
    StreamObserver<ServerSideExtension.BundledRows> sumOfColumn =
                                                    new StreamObserver<ServerSideExtension.BundledRows>() {
      List<Double> params = new ArrayList<Double>();
      @Override
      public void onNext(ServerSideExtension.BundledRows bundledRows) {
        for(ServerSideExtension.Row row : bundledRows.getRowsList()) {
          System.out.println(row.getDuals(0).getNumData());
          params.add(row.getDuals(0).getNumData()); // row=[Col1]
        }
      }

      @Override
      public void onError(Throwable t) {
        t.printStackTrace();
      }

      @Override
      public void onCompleted() {
        System.out.println("onCompleted");
        ServerSideExtension.Dual dual = ServerSideExtension.Dual.newBuilder().setNumData(99.9).build();
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
      return sumOfColumn;
    }
    else if(func_id == 1) {
      return sumOfRows;
    }
    else {
      Status status = Status.UNIMPLEMENTED.withDescription("Method not implemented!");
      responseObserver.onError(status.asRuntimeException());
      return responseObserver;
    }
  }

  @Override
  public void getCapabilities(ServerSideExtension.Empty request, StreamObserver<ServerSideExtension.Capabilities> responseObserver) {
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
    ExtensionServiceImpl service = new ExtensionServiceImpl();
    server = ServerBuilder.forPort(50053).addService(service).intercept(
                                              new SSEInterceptor(service)).build().start();
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
