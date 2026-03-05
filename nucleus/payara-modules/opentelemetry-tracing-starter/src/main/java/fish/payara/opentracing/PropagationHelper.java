/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023-2026 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.opentracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

/**
 * Helper for the most usual propagation scenarios.
 */
public class PropagationHelper implements Scope {
    private final Span span;
    private final Context propagatedContext;
    private Scope spanScope;
    private Scope contextScope;

    private boolean singleThreaded = true;

    private boolean errorReported;

    private PropagationHelper(Span span, Context propagatedContext) {
        this.span = span;
        this.propagatedContext = propagatedContext;
    }

    /**
     * The most usual constellation where we wrap context and span execution synchronously.
     * Span and Context gets installed and then finished upon closing of returned {@code PropagationHelper},
     * as well as Span gets ended with OK status at the end.
     * @param span
     * @param spanContext
     * @return
     */
    public static PropagationHelper start(Span span, Context spanContext) {
        return new PropagationHelper(span, spanContext).start(true);
    }

    /**
     * Start initial processing of a request that will continue processing in multiple threads.
     * Span and Context gets
     * @param span
     * @param spanContext
     * @return
     */
    public static PropagationHelper startMultiThreaded(Span span, Context spanContext) {
        return new PropagationHelper(span, spanContext).start(false);
    }

    /**
     * Install span and context as current for partial execution. Span is not finished upon closing this scope
     * and it needs to be ended explicitly via {@link #end()} method.
     * @return Scope to use in try-with-resources
     */
    public Scope localScope() {
        return new LocalScope();
    }

    public Span span() {
        return this.span;
    }

    private PropagationHelper start(boolean singleThreaded) {
        this.singleThreaded = singleThreaded;
        if (propagatedContext != null) {
            this.contextScope = propagatedContext.makeCurrent();
        }
        this.spanScope = span.makeCurrent();
        return this;
    }

    /**
     * End the span successfully. Should not be used in single-threaded scenario.
     *
     */
    public void end() {
        if (!errorReported) {
            span.end();
            span.setStatus(StatusCode.OK);
        }
    }

    /**
     * End the span with an error. Can be used in both single and multi-threaded
     * scenarios. Ending span twice in single threaded scenario is prevented.
     * @param error
     */
    public void end(Throwable error) {
        if (error == null) {
            end();
        } else {
            errorReported = true;
            span.setStatus(StatusCode.ERROR, error.getMessage());
            span.recordException(error);
        }
    }

    @Override
    public void close() {
        closeContext();
        if (singleThreaded && !errorReported) {
            end();
        }
    }

    public void closeContext() {
        if (spanScope != null) {
            spanScope.close();
            spanScope = null;
        }
        if (contextScope != null) {
            contextScope.close();
            contextScope = null;
        }
    }

    public class LocalScope implements Scope {
        private final Scope localContext;

        private final Scope localSpan;

        private LocalScope() {
            if (propagatedContext != null) {
                this.localContext = propagatedContext.makeCurrent();
            } else {
                this.localContext = null;
            }
            this.localSpan = span.makeCurrent();
        }
        @Override
        public void close() {
            if (localContext != null) {
                localContext.close();
            }
            this.localSpan.close();
        }
    }
}
