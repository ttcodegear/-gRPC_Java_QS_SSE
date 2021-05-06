package SSE_Example;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.Metadata;
import io.grpc.Status;

import qlik.sse.*;

import java.math.BigDecimal;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

class ExtensionServiceImpl extends ConnectorGrpc.ConnectorImplBase {
  private ThreadLocal<Metadata> tl_headers = new ThreadLocal<Metadata>();

  public void setMetadata(Metadata headers) {
    tl_headers.set(headers);
  }

  static class Pair {
    Pair(ServerSideExtension.DataType datatype, ServerSideExtension.Dual dual) {
      this.datatype = datatype;
      this.dual = dual;
    }
    ServerSideExtension.DataType datatype;
    ServerSideExtension.Dual dual;
  }

  private List<Pair> zipLists(List<ServerSideExtension.Parameter> a, List<ServerSideExtension.Dual> b) {
    return IntStream.range(0, Math.min(a.size(), b.size()))
             .mapToObj(i -> new Pair(a.get(i).getDataType(), b.get(i))).collect(Collectors.toList());
  }

  // Function Name | Function Type  | Argument     | TypeReturn Type
  // ScriptEval    | Scalar, Tensor | Numeric      | Numeric
  // ScriptEvalEx  | Scalar, Tensor | Dual(N or S) | Numeric
  // https://docs.groovy-lang.org/latest/html/documentation/#_number_type_suffixes
  private StreamObserver<ServerSideExtension.BundledRows> scriptEval(
                                        ServerSideExtension.ScriptRequestHeader header,
                                        StreamObserver<ServerSideExtension.BundledRows> responseObserver) {
    System.out.println("script=" + header.getScript());
    // パラメータがあるか否かをチェック
    if( header.getParamsList().size() > 0 ) {
      return new StreamObserver<ServerSideExtension.BundledRows>() {
        @Override
        public void onNext(ServerSideExtension.BundledRows bundledRows) {
          List<List<Object>> all_args = new ArrayList<List<Object>>();
          for(ServerSideExtension.Row row : bundledRows.getRowsList()) {
            List<Object> script_args = new ArrayList<Object>();
            List<Pair> zip = zipLists(header.getParamsList(), row.getDualsList());
            for(Pair elm : zip) {
              if( elm.datatype == ServerSideExtension.DataType.NUMERIC ||
                  elm.datatype == ServerSideExtension.DataType.DUAL )
                script_args.add(elm.dual.getNumData());
              else
                script_args.add(elm.dual.getStrData());
            }
            System.out.println("args=" + script_args);
            all_args.add(script_args);
          }
          List<Object> all_results = new ArrayList<Object>();
          for(List<Object> script_args : all_args) {
            double result = Double.NaN;
            try {
              Binding bind = new Binding();
              bind.setVariable("args", script_args);
              Object retobj = (new GroovyShell(bind)).evaluate(header.getScript());
              if(retobj instanceof Number)
                result = ((Number)retobj).doubleValue();
              else if(retobj instanceof String)
                result = Double.parseDouble((String)retobj);
            }
            catch (Exception ex) {
              ex.printStackTrace();
            }
            all_results.add(result);
          }
          ServerSideExtension.BundledRows.Builder response_rows = ServerSideExtension.BundledRows.newBuilder();
          for(Object result : all_results) {
            ServerSideExtension.Dual dual = ServerSideExtension.Dual.newBuilder().setNumData((Double)result).build();
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
    }
    else {
      return new StreamObserver<ServerSideExtension.BundledRows>() {
        @Override
        public void onNext(ServerSideExtension.BundledRows bundledRows) {}
        @Override
        public void onError(Throwable t) {
          t.printStackTrace();
        }
        @Override
        public void onCompleted() {
          double result = Double.NaN;
          try {
            Binding bind = new Binding();
            bind.setVariable("args", new ArrayList<Object>());
            Object retobj = (new GroovyShell(bind)).evaluate(header.getScript());
            if(retobj instanceof Number)
              result = ((Number)retobj).doubleValue();
            else if(retobj instanceof String)
              result = Double.parseDouble((String)retobj);
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
          ServerSideExtension.Dual dual = ServerSideExtension.Dual.newBuilder().setNumData(result).build();
          ServerSideExtension.Row row = ServerSideExtension.Row.newBuilder().addDuals(dual).build();
          ServerSideExtension.BundledRows.Builder builder = ServerSideExtension.BundledRows.newBuilder();
          ServerSideExtension.BundledRows reply = builder.addRows(row).build();
          responseObserver.onNext(reply);
          responseObserver.onCompleted();
        }
      };
    }
  }

  // Function Name   | Function Type | Argument     | TypeReturn Type
  // ScriptAggrStr   | Aggregation   | String       | String
  // ScriptAggrExStr | Aggregation   | Dual(N or S) | String
  private StreamObserver<ServerSideExtension.BundledRows> scriptAggrStr(
                                        ServerSideExtension.ScriptRequestHeader header,
                                        StreamObserver<ServerSideExtension.BundledRows> responseObserver) {
    System.out.println("script=" + header.getScript());
    // パラメータがあるか否かをチェック
    if( header.getParamsList().size() > 0 ) {
      return responseObserver;
    }
    else {
      return new StreamObserver<ServerSideExtension.BundledRows>() {
        @Override
        public void onNext(ServerSideExtension.BundledRows bundledRows) {}
        @Override
        public void onError(Throwable t) {
          t.printStackTrace();
        }
        @Override
        public void onCompleted() {
          String result = "";
          try {
            Binding bind = new Binding();
            bind.setVariable("args", new ArrayList<Object>());
            Object retobj = (new GroovyShell(bind)).evaluate(header.getScript());
            if(retobj instanceof Number)
              result = ((Number)retobj).toString();
            else if(retobj instanceof String)
              result = (String)retobj;
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
          ServerSideExtension.Dual dual = ServerSideExtension.Dual.newBuilder().setStrData(result).build();
          ServerSideExtension.Row row = ServerSideExtension.Row.newBuilder().addDuals(dual).build();
          ServerSideExtension.BundledRows.Builder builder = ServerSideExtension.BundledRows.newBuilder();
          ServerSideExtension.BundledRows reply = builder.addRows(row).build();
          responseObserver.onNext(reply);
          responseObserver.onCompleted();
        }
      };
    }
  }

  private String getFunctionName(ServerSideExtension.ScriptRequestHeader header) {
    ServerSideExtension.FunctionType func_type = header.getFunctionType();
    List<ServerSideExtension.DataType> arg_types = header.getParamsList().stream()
                                         .map(param -> param.getDataType()).collect(Collectors.toList());
    ServerSideExtension.DataType ret_type = header.getReturnType();
/*
    if( func_type == ServerSideExtension.FunctionType.SCALAR ||
        func_type == ServerSideExtension.FunctionType.TENSOR )
      System.out.println("func_type SCALAR TENSOR");
    else if( func_type == ServerSideExtension.FunctionType.AGGREGATION )
      System.out.println("func_type AGGREGATION");

    if( arg_types.size() == 0 )
      System.out.println("arg_type Empty");
    else if( arg_types.stream().allMatch(a -> a == ServerSideExtension.DataType.NUMERIC) )
      System.out.println("arg_type NUMERIC");
    else if( arg_types.stream().allMatch(a -> a == ServerSideExtension.DataType.STRING) )
      System.out.println("arg_type STRING");
    else if( arg_types.stream().map(a -> a).collect(Collectors.toSet()).size() >= 2 ||
             arg_types.stream().allMatch(a -> a == ServerSideExtension.DataType.DUAL) )
      System.out.println("arg_type DUAL");

    if( ret_type == ServerSideExtension.DataType.NUMERIC )
      System.out.println("ret_type NUMERIC");
    else if( ret_type == ServerSideExtension.DataType.STRING )
      System.out.println("ret_type STRING");
*/
    if( func_type == ServerSideExtension.FunctionType.SCALAR || func_type == ServerSideExtension.FunctionType.TENSOR )
      if( arg_types.size() == 0 || arg_types.stream().allMatch(a -> a == ServerSideExtension.DataType.NUMERIC) )
        if( ret_type == ServerSideExtension.DataType.NUMERIC )
          return "ScriptEval";

    if( func_type == ServerSideExtension.FunctionType.SCALAR || func_type == ServerSideExtension.FunctionType.TENSOR )
      if( arg_types.stream().map(a -> a).collect(Collectors.toSet()).size() >= 2 || arg_types.stream().allMatch(a -> a == ServerSideExtension.DataType.DUAL) )
        if( ret_type == ServerSideExtension.DataType.NUMERIC )
          return "ScriptEvalEx";

    if( func_type == ServerSideExtension.FunctionType.AGGREGATION )
      if( arg_types.size() == 0 || arg_types.stream().allMatch(a -> a == ServerSideExtension.DataType.STRING) )
        if( ret_type == ServerSideExtension.DataType.STRING )
          return "ScriptAggrStr";

    if( func_type == ServerSideExtension.FunctionType.AGGREGATION )
      if( arg_types.stream().map(a -> a).collect(Collectors.toSet()).size() >= 2 || arg_types.stream().allMatch(a -> a == ServerSideExtension.DataType.DUAL) )
        if( ret_type == ServerSideExtension.DataType.STRING )
          return "ScriptAggrExStr";

    return "Unsupported Function Name";
  }

  @Override
  public StreamObserver<ServerSideExtension.BundledRows> evaluateScript(
                                        StreamObserver<ServerSideExtension.BundledRows> responseObserver) {
    System.out.println("evaluateScript");
    try {
      // Read gRPC metadata
      byte[] data = tl_headers.get().get(Metadata.Key.of("qlik-scriptrequestheader-bin",
                                                         Metadata.BINARY_BYTE_MARSHALLER));
      ServerSideExtension.ScriptRequestHeader header = ServerSideExtension.ScriptRequestHeader.parseFrom(data);
      String func_name = getFunctionName(header);
      if(func_name.equals("ScriptEval") || func_name.equals("ScriptEvalEx")) {
        return scriptEval(header, responseObserver);
      }
      else if(func_name.equals("ScriptAggrStr") || func_name.equals("ScriptAggrExStr")) {
        return scriptAggrStr(header, responseObserver);
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    Status status = Status.UNIMPLEMENTED.withDescription("Method not implemented!");
    responseObserver.onError(status.asRuntimeException());
    return responseObserver;
  }

  @Override
  public void getCapabilities(ServerSideExtension.Empty request,
                              StreamObserver<ServerSideExtension.Capabilities> responseObserver) {
    System.out.println("getCapabilities");
    ServerSideExtension.Capabilities capabilities = ServerSideExtension.Capabilities.newBuilder()
      .setAllowScript(true)
      .setPluginIdentifier("Simple SSE Test")
      .setPluginVersion("v0.0.1")
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
