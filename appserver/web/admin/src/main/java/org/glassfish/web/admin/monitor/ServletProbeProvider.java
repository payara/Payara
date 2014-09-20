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

import java.security.Principal;
import org.glassfish.external.probe.provider.annotations.Probe;
import org.glassfish.external.probe.provider.annotations.ProbeParam;
import org.glassfish.external.probe.provider.annotations.ProbeProvider;

/**
 * Provider interface for servlet related probes.
 */
@ProbeProvider(moduleProviderName="glassfish", moduleName="web", probeProviderName="servlet")
public class ServletProbeProvider {

    /**
     * Emits notification that a servlet has been initialized.
     *
     * @param servletName the name of the servlet that was initialized
     * @param appName the name of the app to which the servlet belongs
     * @param hostName the name of the virtual server on which the app has 
     * been deployed
     */
    @Probe(name="servletInitializedEvent")
    public void servletInitializedEvent(
        @ProbeParam("servletName") String servletName,
        @ProbeParam("appName") String appName,
        @ProbeParam("hostName") String hostName) {}

    /**
     * Emits notification that a servlet has been destroyed.
     *
     * @param servletName the name of the servlet that was destroyed
     * @param appName the name of the app to which the servlet belongs
     * @param hostName the name of the virtual server on which the app has 
     * been deployed
     */
    @Probe(name="servletDestroyedEvent")
    public void servletDestroyedEvent(
        @ProbeParam("servletName") String servletName,
        @ProbeParam("appName") String appName,
        @ProbeParam("hostName") String hostName) {}

    /**
     * Emits notification that a servlet is about to enter its service
     * method.
     *
     * @param servletName the name of the servlet
     * @param appName the name of the app to which the servlet belongs
     * @param hostName the name of the virtual server on which the app has 
     * been deployed
     */
    @Probe(name="beforeServiceEvent")
    public void beforeServiceEvent(
        @ProbeParam("servletName") String servletName,
        @ProbeParam("appName") String appName,
        @ProbeParam("hostName") String hostName) {}

    /**
     * Emits notification that a servlet has returned from its service
     * method.
     *
     * @param servletName the name of the servlet
     * @param responseStatus the response status
     * @param appName the name of the app to which the servlet belongs
     * @param hostName the name of the virtual server on which the app has 
     * been deployed
     */
    @Probe(name="afterServiceEvent")
    public void afterServiceEvent(
        @ProbeParam("servletName") String servletName,
        @ProbeParam("responseStatus") int responseStatus,
        @ProbeParam("appName") String appName,
        @ProbeParam("hostName") String hostName) {}
}
