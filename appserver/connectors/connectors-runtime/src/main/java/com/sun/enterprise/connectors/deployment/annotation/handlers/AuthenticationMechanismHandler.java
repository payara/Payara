/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.deployment.annotation.handlers;

import com.sun.enterprise.deployment.annotation.context.RarBundleContext;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.OutboundResourceAdapter;
import com.sun.enterprise.deployment.AuthMechanism;
import com.sun.enterprise.deployment.PoolManagerConstants;
import com.sun.enterprise.deployment.annotation.handlers.AbstractHandler;
import com.sun.enterprise.deployment.xml.ConnectorTagNames;
import com.sun.enterprise.util.LocalStringManagerImpl;

import javax.resource.spi.AuthenticationMechanism;
import javax.resource.spi.Connector;
import javax.resource.spi.security.GenericCredential;
import javax.resource.spi.security.PasswordCredential;
import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ietf.jgss.GSSCredential;
import org.glassfish.apf.*;
import org.glassfish.apf.impl.AnnotationUtils;
import org.glassfish.apf.impl.HandlerProcessingResultImpl;
import org.jvnet.hk2.annotations.Service;


/**
 * @author Jagadish Ramu
 */
@Service
@AnnotationHandlerFor(AuthenticationMechanism.class)
public class AuthenticationMechanismHandler extends AbstractHandler {

    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(AuthenticationMechanismHandler.class);

    public HandlerProcessingResult processAnnotation(AnnotationInfo element) throws AnnotationProcessorException {
        AnnotatedElementHandler aeHandler = element.getProcessingContext().getHandler();
        AuthenticationMechanism authMechanism = (AuthenticationMechanism) element.getAnnotation();

        if (aeHandler instanceof RarBundleContext) {
            boolean isConnectionDefinition = hasConnectorAnnotation(element);
            if (isConnectionDefinition) {
                RarBundleContext rarContext = (RarBundleContext) aeHandler;
                ConnectorDescriptor desc = rarContext.getDescriptor();
                if (!desc.getOutBoundDefined()) {
                    OutboundResourceAdapter ora = new OutboundResourceAdapter();
                    desc.setOutboundResourceAdapter(ora);
                }
                OutboundResourceAdapter ora = desc.getOutboundResourceAdapter();
                String[] description = authMechanism.description();
                int authMechanismValue = getAuthMechVal(authMechanism.authMechanism());
                AuthenticationMechanism.CredentialInterface ci = authMechanism.credentialInterface();
                String credentialInterface = ora.getCredentialInterfaceName(ci);
                //XXX: Siva: For now use the first description
                String firstDesc = "";
                if(description.length > 0) {
                    firstDesc = description[0];
                }
                AuthMechanism auth = new AuthMechanism(firstDesc, authMechanismValue, credentialInterface);
                ora.addAuthMechanism(auth);
            } else {
                getFailureResult(element, "Not a @Connector annotation : @AuthenticationMechanism must " +
                        "be specified along with @Connector annotation", true);
            }
        } else {
            getFailureResult(element, "Not a rar bundle context", true);
        }
        return getDefaultProcessedResult();
    }

    private boolean hasConnectorAnnotation(AnnotationInfo element) {
        Class c = (Class) element.getAnnotatedElement();
        return c.getAnnotation(Connector.class) != null;
    }

    public Class<? extends Annotation>[] getTypeDependencies() {
        return getConnectorAnnotationTypes();
    }

    /**
     * @return a default processed result
     */
    protected HandlerProcessingResult getDefaultProcessedResult() {
        return HandlerProcessingResultImpl.getDefaultResult(
                getAnnotationType(), ResultType.PROCESSED);
    }

    /**
     * Set the authentication mechanism value.
     */
    public int getAuthMechVal(String value) {
        int authMechVal;
        if ((value.trim()).equals(ConnectorTagNames.DD_BASIC_PASSWORD))
            authMechVal = PoolManagerConstants.BASIC_PASSWORD;
        else if ((value.trim()).equals(ConnectorTagNames.DD_KERBEROS))
            authMechVal = PoolManagerConstants.KERBV5;
        else throw new IllegalArgumentException("Invalid auth-mech-type");// put this in localStrings...
        return authMechVal;
    }

    private HandlerProcessingResultImpl getFailureResult(AnnotationInfo element, String message, boolean doLog) {
        HandlerProcessingResultImpl result = new HandlerProcessingResultImpl();
        result.addResult(getAnnotationType(), ResultType.FAILED);
        if (doLog) {
            Class c = (Class) element.getAnnotatedElement();
            String className = c.getName();
            Object args[] = new Object[]{
                element.getAnnotation(),
                className,
                message,
            };
            String localString = localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.connectorannotationfailure",
                    "failed to handle annotation [ {0} ] on class [ {1} ], reason : {2}", args);
            logger.log(Level.WARNING, localString);
        }
        return result;
    }
}
