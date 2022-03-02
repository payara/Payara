/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021] Payara Foundation and/or its affiliates. All rights reserved.
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

import static java.util.logging.Level.*;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import fish.payara.samples.grpc.PayaraServiceGrpc.PayaraServiceStub;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class GrpcClient {

    private static final Logger LOGGER = Logger.getLogger(GrpcClient.class.getName());

    private final PayaraServiceStub stub;

    private CountDownLatch latch;
    private AtomicReference<Throwable> error;

    public GrpcClient(URL url) throws URISyntaxException {
        final Channel channel = ManagedChannelBuilder //
                .forAddress(url.getHost(), url.getPort()) //
                .usePlaintext() //
                .build();

        this.stub = PayaraServiceGrpc.newStub(channel);
        this.error = new AtomicReference<>(null);
    }

    public synchronized void communicate() throws InterruptedException {
        latch = new CountDownLatch(1);
        stub.communicate(request("Hello World"), new ResponseObserver());
        latch.await(20, TimeUnit.SECONDS);
    }

    public Throwable getError() {
        return error.get();
    }

    private final class ResponseObserver implements StreamObserver<PayaraResp> {

        @Override
        public void onNext(PayaraResp response) {
            LOGGER.log(INFO, "Response received: \"{0}\".", response.getMessage());
        }

        @Override
        public void onError(Throwable t) {
            LOGGER.log(SEVERE, "Error received", t);
            error.set(t);
            latch.countDown();
        }

        @Override
        public void onCompleted() {
            latch.countDown();
        }

    }

    private static final PayaraReq request(String message) {
        return PayaraReq.newBuilder() //
                .setMessage(message) //
                .build();
    }

}
