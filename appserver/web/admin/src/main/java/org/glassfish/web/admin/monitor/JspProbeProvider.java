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

package org.glassfish.web.admin.monitor;

import javax.servlet.Servlet;
import org.glassfish.external.probe.provider.annotations.Probe;
import org.glassfish.external.probe.provider.annotations.ProbeParam;
import org.glassfish.external.probe.provider.annotations.ProbeProvider;

/**
 * Provider interface for JSP related probes.
 */
@ProbeProvider(moduleProviderName="glassfish", moduleName="web", probeProviderName="jsp")
public class JspProbeProvider {

    /**
     * Emits notification that a JSP has been accessed for the first time
     * and its corresponding Servlet has been loaded and initialized.
     *
     * @param jspUri The path (relative to the root of the application)
     * to the JSP that was loaded
     * @param appName The name of the application to which the JSP belongs
     * @param hostName The name of the virtual server on which the
     * application has been deployed
     */
    @Probe(name="jspLoadedEvent")
    public void jspLoadedEvent(
        @ProbeParam("jspUri") String jspUri,
        @ProbeParam("appName") String appName,
        @ProbeParam("hostName") String hostName) {}

    /**
     * Emits notification that a JSP whose source code has changed since
     * it was first deployed has been accessed again and was recompiled,
     * and its corresponding Servlet reloaded and reinitialized.
     *
     * @param jspUri The path (relative to the root of the application)
     * to the JSP that was reloaded
     * @param appName The name of the application to which the JSP belongs
     * @param hostName The name of the virtual server on which the
     * application has been deployed
     */
    @Probe(name="jspReloadedEvent")
    public void jspReloadedEvent(
        @ProbeParam("jspUri") String jspUri,
        @ProbeParam("appName") String appName,
        @ProbeParam("hostName") String hostName) {}

    /**
     * Emits notification that a JSP is being destroyed, that is, the
     * Servlet corresponding to the JSP is called at its destroy method
     * either because the JSP is being reloaded or because the application
     * to which the JSP belongs is being stopped (for example, as part of its
     * undeployment).
     *
     * @param jspUri The path (relative to the root of the application)
     * to the JSP that was destroyed
     * @param appName The name of the application to which the JSP belongs
     * @param hostName The name of the virtual server on which the
     * application has been deployed
     */
    @Probe(name="jspDestroyedEvent")
    public void jspDestroyedEvent(
        @ProbeParam("jspUri") String jspUri,
        @ProbeParam("appName") String appName,
        @ProbeParam("hostName") String hostName) {}

    /**
     * Emits notification that access to a JSP has resulted in an error.
     *
     * @param jspUri The path (relative to the root of the application)
     * to the JSP that produced the error
     * @param appName The name of the application to which the JSP belongs
     * @param hostName The name of the virtual server on which the
     * application has been deployed
     */
    @Probe(name="jspErrorEvent")
    public void jspErrorEvent(
        @ProbeParam("jspUri") String jspUri,
        @ProbeParam("appName") String appName,
        @ProbeParam("hostName") String hostName) {}
}
