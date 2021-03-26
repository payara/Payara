package fish.payara.samples.grpc;

import fish.payara.samples.grpc.PayaraServiceGrpc.PayaraServiceImplBase;
import io.grpc.stub.StreamObserver;

public class PayaraService extends PayaraServiceImplBase {

    @Override
    public void communicate(PayaraReq request, StreamObserver<PayaraResp> responseObserver) {
        final String message = request.getMessage();
        responseObserver.onNext(response(message));
        responseObserver.onCompleted();
    }

    private static final PayaraResp response(String message) {
        return PayaraResp.newBuilder() //
                .setMessage(message) //
                .build();
    }

}
