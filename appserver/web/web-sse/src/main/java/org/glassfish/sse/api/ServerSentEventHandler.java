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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * A handler that handles Server-Sent Events.
 *
 * @see ServerSentEvent
 * @author Jitendra Kotamraju
 */
public abstract class ServerSentEventHandler {
    protected ServerSentEventConnection connection;

    public enum Status { DONT_RECONNECT, OK }

    /**
     * A callback to indicate that a client connects to receive Server-Sent Events.
     * The application has full access to HTTP request and can decide whether
     * to accept the connection or reject it. In SSE, clients will reconnect
     * if the connection is closed, but can be told to stop reconnecting by
     * returning the appropriate status.
     *
     * <p>
     * Last-Event-ID may be used in determining the status and it can be
     * got using {@code HttpServletRequest.getHeader("Last-Event-ID")}
     *
     * @param request connection request
     * @return Status to accept, or don't reconnect etc
     */
    public Status onConnecting(HttpServletRequest request) {
        return Status.OK;
    }

    /**
     * A callback to indicate that a client connects to receive Server-Sent Events.
     * The application has full access to HTTP request and can decide whether
     * to accept the connection or reject it. In SSE, clients will reconnect
     * if the connection is closed, but can be told to stop reconnecting by
     * returning the appropriate status.
     *
     * @param connection Server-Sert event connection
     * @return Status to accept, or don't reconnect etc
     */
    public void onConnected(ServerSentEventConnection connection) {
        this.connection = connection;
    }

    /**
     * Callback to indicate that the client closed the connection
     */
    public void onClosed() {
    }

}
