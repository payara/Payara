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

import fish.payara.samples.grpc.ejbcomponents.StatelessEjb;
import io.grpc.Status;

import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import java.util.logging.Logger;

@Dependent
public class PayaraService extends PayaraServiceGrpc.PayaraServiceImplBase {

    private final static Logger log = Logger.getLogger(PayaraService.class.getName());

    @EJB
    StatelessEjb statelessEjb;

    /**
     * Implementation of the gRPC 'communicate' Service defined in payara_ejb.proto
     * Gets the message from the request, prints the incoming message to the server log,
     * sets the message as the response and EJB data, then marks the response as complete
     */
    @Override
    public void communicate(fish.payara.samples.grpc.PayaraReq request, io.grpc.stub.StreamObserver<fish.payara.samples.grpc.PayaraResp> responseObserver) {
        final String message = request.getMessage();
        if (message.equals("Error")) {
            responseObserver.onError(Status.PERMISSION_DENIED.asRuntimeException());
            return;
        }
        log.info(String.format("Processing message: %s", message));
        responseObserver.onNext(response(String.format("Client Message: %s. %s", message, statelessEjb.getAccessCount())));
        responseObserver.onCompleted();
    }

    private static final fish.payara.samples.grpc.PayaraResp response(String message) {
        return fish.payara.samples.grpc.PayaraResp.newBuilder()
                .setMessage(message)
                .build();
    }

}