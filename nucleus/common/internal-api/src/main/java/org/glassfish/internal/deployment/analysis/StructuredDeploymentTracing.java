/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */
package org.glassfish.internal.deployment.analysis;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.internal.deployment.DeploymentTracing;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import static org.glassfish.internal.deployment.analysis.TraceContext.Level.APPLICATION;

/**
 * Refined deployment tracing facility for structured capture of stages of deployment.
 * The trace consists of multiple {@linkplan DeploymentSpan spans}, which represent time periods of execution.
 * Stages can be nested.
 * This nesting is represented by {@link TraceContext}, where we expect that in the hierarchy there is at most one
 * active context per level.
 * At instantiation time, {@code APPLICATION} context is created as top-level one.
 * The process finishes by invoking {@link #close()}, which finishes the top-level context.
 * There is set of methods that match the interface of former {@link DeploymentTracing} class, and methods, that allow
 * spans to be obtained, and then finished by invoking {@link DeploymentSpan#close()} method, or via try-with-resources,
 * as they are {@link AutoCloseable}.
 *
 * @author Patrik Dudits
 */
public final class StructuredDeploymentTracing implements AutoCloseable {
    /**
     * Create new instance for application
     *
     * @param appName
     * @return new instance
     */
    public static StructuredDeploymentTracing create(String appName) {
        return new StructuredDeploymentTracing(appName);
    }

    /**
     * Create disabled instance. If tracing is disabled, only root context is captured. Marks do nothing, and dummy span is returned
     * from {@code startSpan}.
     *
     * @param appName
     * @return
     */
    public static StructuredDeploymentTracing createDisabled(String appName) {
        return new StructuredDeploymentTracing(appName).disable();
    }

    private final Clock clock;
    private final TraceContext rootContext;
    private TraceContext currentContext;
    private DeploymentSpan currentSpan;
    private List<DeploymentSpan> spans = new ArrayList<>();
    private boolean disabled;
    private DeploymentSpan disabledSpan;

    private StructuredDeploymentTracing(String appName) {
        this(Clock.systemDefaultZone(), appName);
    }

    StructuredDeploymentTracing(Clock clock, String appName) {
        this.clock = clock;
        this.rootContext = pushContext(APPLICATION, appName);
        startRootSpan(appName);
    }

    public static StructuredDeploymentTracing load(DeploymentContext context) {
        if (context == null) {
            return StructuredDeploymentTracing.createDisabled("temp");
        } else {
            StructuredDeploymentTracing instance = context.getModuleMetaData(StructuredDeploymentTracing.class);
            if (instance == null) {
                return StructuredDeploymentTracing.createDisabled("temp");
            } else {
                return instance;
            }
        }
    }

    /**
     * Start a new span that will be finished by invoking its {@link DeploymentSpan#close()} method
     * @param action action being performed
     * @return closeable span
     */
    public DeploymentSpan startSpan(Enum<?> action) {
        return startSpan(action, null);
    }

    /**
     * Start a new span that will be finished by invoking its {@link DeploymentSpan#close()} method
     * @param action action being performed
     * @param componentName optional component the action is performed upon
     * @return closeable span
     */
    public DeploymentSpan startSpan(Enum<?> action, String componentName) {
        if (disabled) {
            return disabledSpan;
        }
        this.currentSpan = DeploymentSpan.createOpen(clock, currentContext, componentName, action);
        this.spans.add(currentSpan);
        return currentSpan;
    }

    /**
     * Switch to new context and start a span.
     * @param level context level
     * @param contextName context name
     * @param action action being performed
     * @return new span
     * @see #switchToContext(TraceContext.Level, String)
     * @see #startSpan(TraceContext.Level, String, Enum, String)
     */
    public DeploymentSpan startSpan(TraceContext.Level level, String contextName, Enum<?> action) {
        return startSpan(level, contextName, action, null);
    }

    /**
     * Switch to new context and start a span.
     * @param level context level
     * @param contextName context name
     * @param action action being performed
     * @param componentName optional component on which the action is being performed
     * @return new span
     * @see #switchToContext(TraceContext.Level, String)
     * @see #startSpan(TraceContext.Level, String, Enum, String)
     */
    public DeploymentSpan startSpan(TraceContext.Level level, String contextName, Enum<?> action, String componentName) {
        switchToContext(level, contextName);
        return startSpan(action, componentName);
    }

    public SpanSequence startSequence(Enum<?> action) {
        return startSequence(action, null);
    }

    public SpanSequence startSequence(Enum<?> action, String componentName) {
        return new SpanSequence(this, startSpan(action, componentName));
    }


    /**
     * Switch to new context.
     * If context on level specified parameter {@level} is already a parent of current context, the context stack will
     * pop to that level. If name of context then doesn't match, a new sibling context is created, and previous one is
     * closed.
     * If given level is not parent of current context, child context is created.
     *
     * @param level Level of the new context
     * @param name  Name of the level
     * @return current context. For user this object is unusally uninteresting
     */
    public TraceContext switchToContext(TraceContext.Level level, String name) {
        if (disabled) {
            return rootContext;
        }
        if (popUpTo(level) != null) {
            // if there is a parent of same level, and the name doesn't match, start a new context.
            // there can be only one application context, so no siblings are created in such case.
            if (level != APPLICATION && !name.equals(this.currentContext.getName())) {
                // there is context of this level, but for different name, we'll finish this context then
                popContext();
            } else {
                // the name matches, previous span continues
                return this.currentContext;
            }
        }
        return pushContext(level, name);
    }

    /**
     * Compatibility with {@link DeploymentTracing}. Creates span that starts at end of previous span, and ends now.
     *
     * @param mark
     */
    public void addModuleMark(String moduleName, DeploymentTracing.ModuleMark mark) {
        switchToContext(TraceContext.Level.MODULE, moduleName);
        appendSpan(mark, null);
    }

    /**
     * Finish the tracing by closing the root context.
     */
    @Override
    public void close() {
        while (this.currentContext != null) {
            popContext();
        }
        postProcess();
    }

    private void postProcess() {

        Deque<Instant> levelEnds = new ArrayDeque<>();
        for (DeploymentSpan span : spans) {
            // remove any levels this span breaches.
            for(Iterator<Instant> ends = levelEnds.descendingIterator(); ends.hasNext();) {
                Instant end = ends.next();
                // using "not is before" to also eliminate equal timestamps.
                if (!span.getStart().isBefore(end)) {
                    ends.remove();
                } else {
                    // no need to continue, other level marks are surely later than the current one.
                    break;
                }
            }
            levelEnds.add(span.getFinish());
            span.setNestLevel(levelEnds.size());
        }
    }

    public boolean isEnabled() {
        return !disabled;
    }

    public DeploymentTracing register(DeploymentContext deploymentContext) {
        deploymentContext.addModuleMetaData(this);
        if (isEnabled()) {
            DeploymentTracing legacyTracing = new DeploymentTracing(this);
            deploymentContext.addModuleMetaData(legacyTracing);
            return legacyTracing;
        }
        return null;
    }

    /**
     * Compatibility with {@link DeploymentTracing}. Finishes tracing and prints out tabular lists of spans
     */
    public void print(PrintWriter out) {
        close();
        spans.forEach(span -> out.println(span));
    }

    public void print(PrintStream out) {
        // since this goes into log over System.out, let's create single log entry from it
        StringWriter stringOut = new StringWriter();
        print(new PrintWriter(stringOut));
        out.println(stringOut.toString());
    }

    /**
     * Compatibility with {@link DeploymentTracing}. Creates span that starts at end of previous span, and ends now.
     *
     * @param mark
     */
    public void addApplicationMark(DeploymentTracing.Mark mark) {
        popUpTo(APPLICATION);
        appendSpan(mark, null);
    }

    /**
     * Compatibility with {@link DeploymentTracing}. Creates span that starts at end of previous span, and ends now.
     *
     * @param mark
     */
    public void addContainerMark(String name, DeploymentTracing.ContainerMark mark) {
        switchToContext(TraceContext.Level.CONTAINER, name);
        appendSpan(mark, null);
    }

    private DeploymentSpan startRootSpan(String appName) {
        return startSpan(null, null);
    }

    private StructuredDeploymentTracing disable() {
        this.disabled = true;
        this.disabledSpan = DeploymentSpan.createOpen(clock, rootContext, "dummy", null);
        return this;
    }

    // this is used by marks - they represent finished action and therefore build closed spans relative to the last span
    private DeploymentSpan appendSpan(Enum<?> action, String componentName) {
        if (disabled) {
            return disabledSpan;
        }
        this.currentSpan = DeploymentSpan.createClosed(this.currentSpan, clock, currentContext, componentName, action);
        this.spans.add(currentSpan);
        return currentSpan;
    }

    private TraceContext pushContext(TraceContext.Level level, String contextName) {
        this.currentContext = new TraceContext(this.currentContext, level, contextName, clock);
        return this.currentContext;
    }

    private TraceContext popContext() {
        if (this.currentContext != null) {
            TraceContext head = this.currentContext;
            this.currentContext = head.pop();
            return head;
        } else {
            return null;
        }
    }

    private TraceContext popUpTo(TraceContext.Level level) {
        if (this.currentContext == null || !this.currentContext.isWithin(level)) {
            // there's no parent of such level, so do nothing, or we'd loose all of context. null signals it cannot be popped
            return null;
        }
        while (!this.currentContext.is(level)) {
            popContext();
        }
        return this.currentContext;
    }

}
