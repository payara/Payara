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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.glassfish.web.valve;

import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;

import javax.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Adapter valve for wrapping a GlassFish-style valve that was compiled
 * against the "old" org.apache.catalina.Valve interface from GlassFish
 * releases prior to V3 (which has been renamed to
 * org.glassfish.web.valve.GlassFishValve in GlassFish V3).
 *
 * @author jluehe
 */
public class GlassFishValveAdapter implements GlassFishValve {

    // The wrapped GlassFish-style valve to which to delegate
    private final Valve gfValve;

    private final Method invokeMethod;
    private final Method postInvokeMethod;

    /**
     * Constructor.
     *
     * @param gfValve The GlassFish valve to which to delegate
     */
    public GlassFishValveAdapter(Valve gfValve) throws Exception {
        this.gfValve = gfValve;
        invokeMethod = gfValve.getClass().getMethod("invoke", Request.class, Response.class);
        postInvokeMethod = gfValve.getClass().getMethod("postInvoke", Request.class, Response.class);
    }

    @Override
    public String getInfo() {
        return gfValve.getInfo();
    }

    /**
     * Delegates to the invoke() of the wrapped GlassFish-style valve.
     */
    @Override
    public int invoke(Request request, Response response) throws IOException, ServletException {
        try {
            return ((Integer) invokeMethod.invoke(gfValve, request, response));
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Delegates to the postInvoke() of the wrapped GlassFish-style valve.
     */
    @Override
    public void postInvoke(Request request, Response response) throws IOException, ServletException {
        try {
            postInvokeMethod.invoke(gfValve, request, response);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
