package SSE_Example;

import io.grpc.ServerInterceptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.Metadata;

import qlik.sse.*;

public class SSEInterceptor implements ServerInterceptor {
  private ExtensionServiceImpl service;

  public SSEInterceptor(ExtensionServiceImpl service) {
    this.service = service;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                                                   ServerCall<ReqT,RespT> call,
                                                   Metadata headers,
                                                   ServerCallHandler<ReqT, RespT> next) {
    service.setMetadata(headers);
    SimpleForwardingServerCall forwarding = new SimpleForwardingServerCall<ReqT, RespT>(call) {
      @Override
      public void sendHeaders(Metadata resp_headers) {
        try {
          // Disable caching
          byte[] data = headers.get(Metadata.Key.of("qlik-functionrequestheader-bin",
                                                    Metadata.BINARY_BYTE_MARSHALLER));
          if(data != null)
            if(ServerSideExtension.FunctionRequestHeader.parseFrom(data).getFunctionId() >= 0)
              resp_headers.put(Metadata.Key.of("qlik-cache", Metadata.ASCII_STRING_MARSHALLER),
                                               "no-store");
        }
        catch (Exception ex) {
        }
        super.sendHeaders(resp_headers);
      }
    };
    return next.startCall(forwarding, headers);
  }
}
