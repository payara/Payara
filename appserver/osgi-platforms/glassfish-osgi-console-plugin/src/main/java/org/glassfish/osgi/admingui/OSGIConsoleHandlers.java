/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.osgi.admingui;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import org.glassfish.internal.api.Globals;
import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

/**
 * handler to get instance's host and port information which is used to
 * form the http url for osgi-console.
 * 
 * @author mohit
 */
public class OSGIConsoleHandlers {

    private static String http_port = "HTTP_LISTENER_PORT";
    private static String consolePath = "/osgi/system/console/bundles";

    @Handler(id = "getConsoleUrl",
    input = {
        @HandlerInput(name = "instanceName", type = String.class, required = true)},
    output = {
        @HandlerOutput(name = "consoleUrl", type = String.class)})
    public static void getConsoleUrl(HandlerContext handlerCtx) {
        String instanceName = (String) handlerCtx.getInputValue("instanceName");

        Domain domain = Globals.get(Domain.class);
        Server server = domain.getServerNamed(instanceName);

        String port = null;
        SystemProperty httpPort = server.getSystemProperty(http_port);
        if(httpPort != null) {
            port = httpPort.getValue();
        } else {
            //if port is not set as system property, get it from config
            Config cfg = server.getConfig();
            SystemProperty httpConfigPort = cfg.getSystemProperty(http_port);
            if(httpConfigPort != null){
                port = httpConfigPort.getValue();
            }
        }

        if(port == null) {
            throw new RuntimeException("Not able to get HTTP_LISTENER_PORT " +
                    "for instance : " + instanceName);
        }

        Node node = domain.getNodeNamed(server.getNodeRef());
        String host = node.getNodeHost();
        String consoleUrl = "http://" + host + ":" + port + consolePath;
        
        handlerCtx.setOutputValue("consoleUrl", consoleUrl);
    }

}
