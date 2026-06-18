/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.samples.remote.ejb.tracing.server;

import fish.payara.samples.remote.ejb.tracing.EjbRemote;
import fish.payara.microprofile.telemetry.tracing.Traced;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Random;


@Stateless
public class Ejb implements EjbRemote {

    /**
     * This method should not be traced, but the baggage items should still be available.
     *
     * @return The current baggage items
     */
    @Override
    public String nonAnnotatedMethod() {
        randomSleep();
        Baggage baggage = Baggage.builder()
                .put("Wibbles", "Wobbles")
                .put("Nibbles", "Nobbles")
                .build();
        try (Scope scope = baggage.storeInContext(Context.current()).makeCurrent()) {
            return getBaggageItems();
        }

    }

    /**
     * This method should be traced with a custom name
     *
     * @return The current baggage items
     */
    @Override
    @Traced(operationName = "customName")
    public String annotatedMethod() {
        randomSleep();
        Baggage baggage = Baggage.builder()
                .put("Wibbles", "Wobbles")
                .build();
        try (Scope scope = baggage.storeInContext(Context.current()).makeCurrent()) {
            return getBaggageItems();
        }
    }

    /**
     * This method itself should not be traced.
     *
     * @return The current baggage items
     */
    @Override
    @Traced(false)
    public String shouldNotBeTraced() {
        randomSleep();
        Baggage baggage = Baggage.builder()
                .put("Wibbles", "Wobbles")
                .put("Nibbles", "Nobbles")
                .put("Bibbles", "Bobbles")
                .build();
        try (Scope scope = baggage.storeInContext(Context.current()).makeCurrent()) {
            return getBaggageItems();
        }
    }

    @Override
    public String editBaggageItems() {
        randomSleep();
        Baggage initial = Baggage.builder()
                .put("Wibbles", "Wobbles")
                .put("Nibbles", "Nobbles")
                .put("Bibbles", "Bobbles")
                .build();
        try (Scope scope = initial.storeInContext(Context.current()).makeCurrent()) {
            Baggage edited = Baggage.current().toBuilder()
                    .put("Wibbles", "Wabbles")
                    .put("Nibbles", "Nabbles")
                    .put("Bibbles", "Babbles")
                    .build();
            try (Scope scope2 = edited.storeInContext(Context.current()).makeCurrent()) {
                return getBaggageItems();
            }
        }
    }

    private void randomSleep() {
        try {
            Thread.sleep(new Random().nextInt(5000));
        } catch (InterruptedException ie) {
            // om nom nom
        }
    }

    private String getBaggageItems() {
        StringBuilder sb = new StringBuilder("\n");
        Baggage.current().asMap().forEach((key, entry) ->
                sb.append(key).append(" : ").append(entry.getValue()).append("\n"));
        return sb.toString();
    }
}
