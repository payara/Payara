/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright holder.
 */
package fish.payara.opentracing;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 * Registers a {@link SpanProcessor} that stamps {@code thread.id} and {@code thread.name}
 * on every span at start time. This makes the executing thread visible in APM UIs, which
 * is especially valuable when concurrent tasks run on different worker threads while
 * appearing as siblings under the same parent span in the trace waterfall.
 *
 * <p>{@link ContractsProvided} is required because {@link AutoConfigurationCustomizerProvider}
 * is a third-party OTel interface that is not annotated with HK2's {@code @Contract}.
 * Without it, HK2's metadata generator does not add the interface as a lookup contract
 * and {@code IterableProvider&lt;AutoConfigurationCustomizerProvider&gt;} finds nothing.
 */
@Service
@ContractsProvided(AutoConfigurationCustomizerProvider.class)
class ThreadAttributesProvider implements AutoConfigurationCustomizerProvider {

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        autoConfiguration.addTracerProviderCustomizer(
                (builder, config) -> builder.addSpanProcessor(new ThreadStampingProcessor()));
    }

    /** Stamps the current thread's id and name on every new span. */
    static final class ThreadStampingProcessor implements SpanProcessor {

        @Override
        public void onStart(Context parentContext, ReadWriteSpan span) {
            Thread t = Thread.currentThread();
            span.setAttribute("thread.id", t.threadId());
            span.setAttribute("thread.name", t.getName());
        }

        @Override public boolean isStartRequired() { return true; }
        @Override public void onEnd(ReadableSpan span) {}
        @Override public boolean isEndRequired() { return false; }
    }
}
