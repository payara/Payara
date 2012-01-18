/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.sse.api;

/**
 * Represents a Server-Sent Event data.
 *
 * <p>
 * {@code new ServerSentEventData().data("YHOO").data("+2").data("10)} would send
 * the following event (followed by blank line):
 * <pre>
 * data:YHOO
 * data:+2
 * data:10
 *
 * </pre>
 *
 * <p>
 * {@code new ServerSentEventData().comment("test stream")} would send the
 * following event (followed by blank line):
 * <pre>
 * :test stream
 *
 * </pre>
 *
 * <p>
 * {@code new ServerSentEventData().data("first event").id("1")} would send the
 * following event (followed by blank line):
 * <pre>
 * data:first event
 * id:1
 * </pre>
 *
 * <p>
 * {@code new ServerSentEventData().event("add").data("73857293")} would send the
 * following event (followed by blank line):
 * <pre>
 * event:add
 * data:73857293
 *
 * </pre>
 *
 * @author Jitendra Kotamraju
 */
public final class ServerSentEventData {
    
    private final StringBuilder strBuilder = new StringBuilder();

    public ServerSentEventData comment(String comment) {
        strBuilder.append(':');
        strBuilder.append(comment);
        strBuilder.append('\n');
        return this;
    }

    public ServerSentEventData data(String data) {
        strBuilder.append("data:");
        strBuilder.append(data);
        strBuilder.append('\n');
        return this;
    }

    public ServerSentEventData data() {
        strBuilder.append("data");
        strBuilder.append('\n');
        return this;
    }

    public ServerSentEventData event(String event) {
        strBuilder.append("event:");
        strBuilder.append(event);
        strBuilder.append('\n');
        return this;
    }

    public ServerSentEventData id(String id) {
        strBuilder.append("id:");
        strBuilder.append(id);
        strBuilder.append('\n');
        return this;
    }

    public ServerSentEventData id() {
        strBuilder.append("id");
        strBuilder.append('\n');
        return this;
    }

    public ServerSentEventData retry(int retry) {
        strBuilder.append("retry:");
        strBuilder.append(String.valueOf(retry));
        strBuilder.append('\n');
        return this;
    }

    @Override
    public String toString() {
        return strBuilder.toString();
    }
}
