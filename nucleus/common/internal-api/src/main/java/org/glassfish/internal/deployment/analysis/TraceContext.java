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
import java.time.Instant;

/**
 * Context identifies the hierarchical nesting of the spans.
 *
 */
public class TraceContext {
    private final TraceContext parent;
    private final Level level;
    private final String name;
    private final Clock clock;
    private final Instant start;
    private Instant finish;


    public enum Level {
        APPLICATION, MODULE, CONTAINER, ENGINE;
    }

    TraceContext(TraceContext parent, Level level, String name, Clock clock) {
        this.start = clock.instant();
        this.parent = parent;
        this.level = level;
        this.name = name;
        this.clock = clock;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getFinish() {
        return finish;
    }

    public Level getLevel() {
        return level;
    }

    public TraceContext getParent() {
        return parent;
    }

    public boolean is(Level level) {
        return level == null || level == this.level;
    }

    public boolean isWithin(Level level) {
        return level == this.level || (parent != null && parent.isWithin(level));
    }

    public String getName() {
        return name;
    }

    public String getLevelName(Level level) {
        if (level == this.level) {
            return name;
        }
        if (parent != null) {
            return parent.getLevelName(level);
        }
        return "";
    }

    void close() {
        if (this.finish == null) {
            this.finish = clock.instant();
        }
    }

    TraceContext pop() {
        close();
        return this.parent;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    void toString(StringBuilder sb) {
        if (parent != null) {
            parent.toString(sb);
            sb.append(" > ");
        }
        sb.append(level).append(" ").append(name);
    }
}
