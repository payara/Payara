/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */
package fish.payara.samples.otel.annotation;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class SpanBean {

    private static final Logger LOG = Logger.getLogger(SpanBean.class.getName());

    @Inject
    SpanChildBean spanChildBean;

    @WithSpan
    public void span() {
        LOG.log(Level.INFO, "invoking span");
    }

    @WithSpan("definedName")
    public void spanName() {
        LOG.log(Level.INFO, "invoking spanName");
    }

    @WithSpan(kind = SpanKind.SERVER)
    public void spanKind() {
        LOG.log(Level.INFO, "invoking spanKind");
    }

    @WithSpan
    public void spanArgs(@SpanAttribute(value = "customStringAttribute") String attr1,
                         @SpanAttribute(value = "customBooleanAttribute") boolean attr2,
                         @SpanAttribute(value = "customIntegerAttribute") int attr3,
                         @SpanAttribute String woSpanAttributeValue,
                         String noName) {
        LOG.log(Level.INFO, "invoking spanArgs with spanAttribute");
    }

    @WithSpan
    public void spanChild() {
        LOG.log(Level.INFO, "invoking spanChild");
        spanChildBean.spanChild();
    }

    @WithSpan
    public CompletionStage<String> asyncSpan() {
        var result = new CompletableFuture<String>();
        new Thread(() -> {
            try {
                Thread.sleep(500);
                result.complete("OK");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        return result;
    }

    @WithSpan
    public CompletionStage<String> asyncExceptionSpan() {
        var result = new CompletableFuture<String>();
        new Thread(() -> {
            try {
                Thread.sleep(500);
                result.completeExceptionally(new IllegalArgumentException("exception being throw"));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        return result;
    }

    @ApplicationScoped
    public static class SpanChildBean {
        @WithSpan
        public void spanChild() {

        }
    }
}
