/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.valve;

import org.apache.catalina.CometEvent;
import org.apache.catalina.Valve;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Tomcat-style wrapper valve around GlassFish-style valve.
 *
 * This allows Tomcat- and GlassFish-style valves to be added to a 
 * pipeline in arbitrary order.
 *
 * @author jluehe
 */
public class TomcatValveAdapter implements Valve {

    // The next valve in the pipeline to be invoked
    private Valve next = null;

    // The wrapped GlassFish-style valve to which to delegate
    private GlassFishValve gfValve;

    /**
     * Constructor.
     *
     * @param gfValve The GlassFish-style valve to wrap
     */
    public TomcatValveAdapter(GlassFishValve gfValve) {
        this.gfValve = gfValve;
    }

    public String getInfo() {
        return gfValve.getInfo();
    }

    public Valve getNext() {
        return next;
    }

    public void setNext(Valve valve) {
        this.next = valve;
    }

    public void backgroundProcess() {
        // Deliberate no-op
    }

    /**
     * Delegates to the invoke() and postInvoke() methods of the wrapped
     * GlassFish-style valve.
     */
    public void invoke(org.apache.catalina.connector.Request request,
                       org.apache.catalina.connector.Response response)
            throws IOException, ServletException {
        int rc = gfValve.invoke(request, response);
        if (rc != GlassFishValve.INVOKE_NEXT) {
            return;
        }
        getNext().invoke(request, response);
        gfValve.postInvoke(request, response);
    }

    public void event(org.apache.catalina.connector.Request request,
                      org.apache.catalina.connector.Response response,
                      CometEvent event)
            throws IOException, ServletException {
        // Deliberate no-op
    }

}
