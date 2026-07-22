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
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
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
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.opentracing;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 * Bridge OTEL traces to Payara Request Service if the service is enabled at time of Sdk creation
 */
@Service
@ContractsProvided(AutoConfigurationCustomizerProvider.class)
class RequestTracingBridgeProvider implements AutoConfigurationCustomizerProvider {
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    RequestTracingServiceConfiguration requestTracingServiceConfiguration;

    @Inject
    IterableProvider<RequestTracingService> requestTracingServiceHandle;

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        if (Boolean.parseBoolean(requestTracingServiceConfiguration.getEnabled())) {
            autoConfiguration.addTracerProviderCustomizer((builder, config) ->
                builder.addSpanProcessor(new LazyRequestTracingProcessor())
            );
        }
    }

    class LazyRequestTracingProcessor implements SpanProcessor {
        private SpanProcessor delegate;


        @Override
        public void onStart(Context parentContext, ReadWriteSpan span) {
            if (delegate == null) {
                return;
            }
            delegate.onStart(parentContext, span);
        }

        @Override
        public boolean isStartRequired() {
            if (delegate == null) {
                return reevaluate();
            }
            return false;
        }

        private boolean reevaluate() {
            if (!requestTracingServiceHandle.getHandle().isActive()) {
                return false;
            }
            delegate = new PayaraRequestTracingProcessor(requestTracingServiceHandle.get());
            return true;
        }

        @Override
        public void onEnd(ReadableSpan span) {
            if (delegate == null) {
                return;
            }
            delegate.onEnd(span);
        }

        @Override
        public boolean isEndRequired() {
            if (delegate == null) {
                return false;
            }
            return delegate.isEndRequired();
        }

        @Override
        public CompletableResultCode shutdown() {
            if (delegate != null) {
                return delegate.shutdown();
            }
            return SpanProcessor.super.shutdown();
        }

        @Override
        public CompletableResultCode forceFlush() {
            if (delegate != null) {
                return delegate.forceFlush();
            }
            return SpanProcessor.super.forceFlush();
        }
    }
}
