/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package org.glassfish.deployapi;

import org.glassfish.deployment.client.ServerConnectionIdentifier;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;

import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Concrete implementation of the JSR 88 DeploymentFactory interface.
 *
 * @author dochez
 * @author tjquinn
 * @author David Matejcek
 */
public class SunDeploymentFactory implements DeploymentFactory {

    private static LocalStringManagerImpl xlocalStrings =
        new LocalStringManagerImpl(SunDeploymentFactory.class);

    //The following URISTRING is what we supported in PE Beta.  Keeping it for
    //backward compatibility
    private final static String PE_BETA_URISTRING = "deployer:Sun:S1AS::"; // NOI18N

    //The following URISTRINNG is what we use for PE FCS and in the future
    private final static String DEFAULT_URISTRING = "deployer:Sun:AppServer::"; // NOI18N
    private final static String HTTPS = "https";
    private final static String URI_SEPARATOR = ":";// NOI18N
    private final static String LOCAL_HOST = "localhost";// NOI18N
    private final static int HOST_PORT = 4848; // default DAS port

    private final static String[] supportedURIs = {PE_BETA_URISTRING, DEFAULT_URISTRING};

    private static final Logger deplLogger = org.glassfish.deployment.client.AbstractDeploymentFacility.deplLogger;

    @LogMessageInfo( //
        message = "Deployment manager load failure.  Unable to find {0}",
        cause = "A deployment manager is not available.",
        action = "Correct the reference to the deployment manager.",
        level = "SEVERE")
    private static final String NO_DEPLOYMENT_MANAGER = "AS-DEPLOYMENT-04019";

    @Override
    public DeploymentManager getDeploymentManager(String uri, String username, String password)
        throws DeploymentManagerCreationException {

        if (!handlesURI(uri)) {
            return null;
        }
        ServerConnectionIdentifier hostInfo = null;
        try {
            hostInfo = parseURIForHostInfo(uri);
        } catch(Exception ex) {
            DeploymentManagerCreationException e = new DeploymentManagerCreationException(
            xlocalStrings.getLocalString(
            "enterprise.deployapi.spi.wronghostidentifier",
            "Wrong host identifier in uri {0} ", new Object[] { uri }));
            e.initCause(ex);
            throw e;
        }
        try {
            hostInfo.setUserName(username);
            hostInfo.setPassword(password);
            DeploymentManager answer = null;

            answer = new SunDeploymentManager(hostInfo);
            return answer;
        } catch(Throwable t) {
            DeploymentManagerCreationException e = new DeploymentManagerCreationException(xlocalStrings.getLocalString(
            "enterprise.deployapi.spi.exceptionwhileconnecting", //NOI18N
            "Exception while connecting to {0} : {1}", new Object[] { uri, t.getMessage() })); //NOI18N
            e.initCause(t);
            throw e;
        }
    }


    /**
     * Return a <tt>disconnected</tt> DeploymentManager instance.
     *
     * @param uri the uri of the DeploymentManager to return.
     * @return A DeploymentManager <tt>disconnected</tt> instance or null if uri is not supported
     */
    @Override
    public DeploymentManager getDisconnectedDeploymentManager(String uri) {
        if (handlesURI(uri)) {
            return new SunDeploymentManager();
        }
        return null;
    }

    @Override
    public String getDisplayName() {
        return xlocalStrings.getLocalString(
                "enterprise.deployapi.spi.DisplayName",
                "Sun Java System Application Server");
    }

    @Override
    public String getProductVersion() {
        return xlocalStrings.getLocalString("enterprise.deployapi.spi.ProductVersion", "9.0");
    }

    @Override
    public boolean handlesURI(String uri) {
        if (deplLogger.isLoggable(Level.FINE)) {
            deplLogger.fine("handlesURI: URI [" + uri + "]");// NOI18N
        }

        if (uri != null) {
            try {
                parseURIForHostInfo(uri);
                return true;
            } catch (Exception ex) {
                deplLogger.log(Level.SEVERE, NO_DEPLOYMENT_MANAGER, uri);
            }
        }
        return false;
    }

    /**
     * @param uri must not be null
     * @return the host name/port from the URI passed see JSR88 paragraph 9.2.3
     * @throws Exception invalid uri
     */
    public ServerConnectionIdentifier parseURIForHostInfo(String uri) throws Exception {
        final String targetURI = toTargetURI(uri);

        // if the URI does not contain DEFAULT_URISTRINNG or PE_BETA_URISTRING,
        // then the URI is not valid.
        if (targetURI == null) {
            throw new Exception(xlocalStrings.getLocalString(
            "enterprise.deployapi.spi.invaliduri", // NOI18N
            "Invalid URI"));    // NOI18N
        }

        ServerConnectionIdentifier sci = new ServerConnectionIdentifier();

        if(uri.length() == targetURI.length()) {
            sci.setHostName(LOCAL_HOST);
            sci.setHostPort(HOST_PORT);
        } else {
            String reminder = uri.substring(targetURI.length());
            String[] splitted = reminder.split(URI_SEPARATOR);
            if (splitted.length<2) {
                throw new Exception(xlocalStrings.getLocalString(
                    "enterprise.deployapi.spi.invaliduri", // NOI18N
                    "Invalid URI"));    // NOI18N
            }
            if ("".equals(splitted[0])) {
                sci.setHostName(LOCAL_HOST);
            } else {
                sci.setHostName(splitted[0]);
            }
            if ("".equals(splitted[1])) {
                sci.setHostPort(HOST_PORT);
            } else {
                sci.setHostPort(Integer.parseInt(splitted[1]));
            }

            if (splitted.length>2) {
                if (HTTPS.equals(splitted[2])) {
                    sci.setSecure(true);
                }
            }
        }
        return sci;
    }

    private String toTargetURI(final String uri) {
        if (uri == null) {
            return null;
        }
        for (String supportedURI : supportedURIs) {
            if (uri.indexOf(supportedURI) == 0) {
                return supportedURI;
            }
        }
        return null;
    }
}
