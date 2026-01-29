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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * Span is a duration of time an event took place.
 * Its position in the hierarchy is identified by {@link TraceContext}, the action being performed by any enum value.
 * Optionally it can contain the name of the component on which the action is performed.
 */
public class DeploymentSpan implements AutoCloseable {
    static final DateTimeFormatter NS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.nnnnnnnnn").withZone(ZoneId.systemDefault());
    private final Clock clock;
    final TraceContext context;
    private final String componentName;
    private final Enum<?> action;
    private final Instant start;
    private Instant finish;
    int nestLevel = 0;

    private DeploymentSpan(Clock clock, TraceContext context, String componentName, Enum<?> action) {
        this.start = clock.instant();
        this.clock = clock;
        this.context = context;
        this.componentName = componentName;
        this.action = action;
    }

    private DeploymentSpan(Instant start, Instant finish, TraceContext context, String componentName, Enum<?> action) {
        this.start = start;
        this.clock = null;
        this.finish = finish;
        this.context = context;
        this.componentName = componentName;
        this.action = action;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getFinish() {
        if (finish == null) {
            if (context == null) {
                return null;
            } else {
                return context.getFinish();
            }
        } else {
            return finish;
        }
    }

    public Duration getDuration() {
        Instant finish = getFinish();
        return finish != null ? Duration.between(getStart(), finish) : null;
    }

    boolean isOpen() {
        return finish == null;
    }

    public Enum<?> getAction() {
        return action;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        NS_FORMATTER.formatTo(start, sb);
        sb.append('\t');
        Instant f = getFinish();
        if (f != null) {
            NS_FORMATTER.formatTo(f, sb);
        }
        sb.append('\t')
            .append(getNestLevel())
            .append('\t');
        if (context != null) {
            sb.append(context.getLevelName(TraceContext.Level.CONTAINER))
                .append('\t')
                .append(context);
        } else {
            sb.append('\t');
        }
        sb.append('\t').append(action).append('\t');
        if (componentName != null) {
            sb.append(componentName);
        }
        sb.append('\t');
        Duration duration = getDuration();
        if (duration != null) {
            sb.append(duration.toMillis());
        }
        return sb.toString();
    }

    /**
     * Close the current span. This marks the end of the span.
     * @throws Exception
     */
    @Override
    public void close() {
        if (this.clock == null) {
            throw new IllegalArgumentException("DeploymentSpan is already created closed");
        }
        if (this.finish != null) {
            return; // we're already closed, that's ok
        }
        this.finish = clock.instant();
    }

    /**
     * Determined level of nesting.
     * @return
     */
    public int getNestLevel() {
        return nestLevel;
    }

    /**
     * Set determined level of nesting. Post-processing will set the level from inspection of all of the spans
     * @param nestLevel
     */
    void setNestLevel(int nestLevel) {
        this.nestLevel = nestLevel;
    }

    /**
     * Create an open span (that it meant to be closed later)
     * @param clock the clock used to determine start and finish time
     * @param context context of the span
     * @param componentName optional component name within the context, that this span refers to
     * @param action action being performed
     * @return new instance
     */
    static DeploymentSpan createOpen(Clock clock, TraceContext context, String componentName, Enum<?> action) {
        return new DeploymentSpan(clock, context, componentName, action);
    }

    /**
     * Create closed span, an information on action that was performed already
     * @param previous previous span, it's end will serve as start of this span. If null, start of the context will be used
     * @param clock clock to determine finish time. The span will finish at time of creation
     * @param context context of the span
     * @param componentName optional component name within the context, that this span refers to
     * @param action action being perfomed
     * @return new instance
     */
    static DeploymentSpan createClosed(DeploymentSpan previous, Clock clock, TraceContext context, String componentName, Enum<?> action) {
        if (previous != null) {
            Instant start = previous.getFinish();
            if (start == null) {
                start = context.getStart();
            }
            return new DeploymentSpan(start, clock.instant(), context, componentName, action);
        } else {
            return new DeploymentSpan(context.getStart(), clock.instant(), context, componentName, action);
        }
    }
}
