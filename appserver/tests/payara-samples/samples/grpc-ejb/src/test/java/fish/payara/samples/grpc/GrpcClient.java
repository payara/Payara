/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.samples.grpc;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GrpcClient {

    private static final Logger LOGGER = Logger.getLogger(GrpcClient.class.getName());
    private PayaraServiceGrpc.PayaraServiceStub stub;
    private CountDownLatch latch;
    private AtomicReference<Throwable> error;

    public GrpcClient(URL url) {
        final Channel channel = ManagedChannelBuilder.forAddress(url.getHost(), url.getPort()).usePlaintext().build();
        this.stub = PayaraServiceGrpc.newStub(channel);
        this.error = new AtomicReference<>(null);
    }

    /**
     * Call the gRPC service, add our message to the request and add a ResponseObserver
     */
    public void communicate(String message) throws InterruptedException {
        latch = new CountDownLatch(1); //Wait until the communication finish with the current thread
        stub.communicate(request(message), new ResponseObserver()); //calling service and adding a ResponseObserver to process response
        latch.await(10, TimeUnit.SECONDS);
    }


    public Throwable getError() {
        return error.get();
    }

    /**
     * Used to create a request with a String message
     */
    private static final PayaraReq request(String message) {
        return PayaraReq.newBuilder().setMessage(message).build();
    }

    private final class ResponseObserver implements StreamObserver<PayaraResp> {

        /**
         * Process the service response. Log the response from the service to the client console
         */
        @Override
        public void onNext(PayaraResp response) {
            LOGGER.log(Level.INFO, "Response received: \"{0}\".", response.getMessage());
        }


        /**
         * Process any errors
         */
        @Override
        public void onError(Throwable t) {
            LOGGER.log(Level.INFO, "Error received", t);
            error.set(t);
            latch.countDown();
        }

        /**
         * Used to indicate the communication is complete for the current thread
         */
        @Override
        public void onCompleted() {
            latch.countDown();
        }
    }

}
