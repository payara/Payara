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

package com.sun.enterprise.web.jsp;

import com.sun.enterprise.web.VirtualServer;
import com.sun.enterprise.web.WebModule;
import org.glassfish.jsp.api.JspProbeEmitter;
import org.glassfish.web.admin.monitor.JspProbeProvider;

/**
 * Implementation of JspProbeEmitter interface that delegates each probe
 * event to the JspProbeProvider.
 *
 * @author jluehe
 */
public class JspProbeEmitterImpl implements JspProbeEmitter {

    private String monitoringNodeName;

    // The id of the virtual server on which the web module has been
    // deployed
    private String vsId;

    private JspProbeProvider jspProbeProvider;

    /**
     * Constructor.
     *
     * @param webModule the web module on whose behalf this
     * JspProbeEmitterImpl emits jsp related probe events
     */
    public JspProbeEmitterImpl(WebModule webModule) {
        this.monitoringNodeName = webModule.getMonitoringNodeName();
        if (webModule.getParent() != null) {
            this.vsId = ((VirtualServer) webModule.getParent()).getID();
        }
        this.jspProbeProvider = webModule.getWebContainer().getJspProbeProvider();
    }

    public void jspLoadedEvent(String jspUri) {
        jspProbeProvider.jspLoadedEvent(jspUri, monitoringNodeName, vsId);
    }

    public void jspReloadedEvent(String jspUri) {
        jspProbeProvider.jspReloadedEvent(jspUri, monitoringNodeName, vsId);
    }

    public void jspDestroyedEvent(String jspUri) {
        jspProbeProvider.jspDestroyedEvent(jspUri, monitoringNodeName, vsId);
    }

    public void jspErrorEvent(String jspUri) {
        jspProbeProvider.jspErrorEvent(jspUri, monitoringNodeName, vsId);
    }
}
