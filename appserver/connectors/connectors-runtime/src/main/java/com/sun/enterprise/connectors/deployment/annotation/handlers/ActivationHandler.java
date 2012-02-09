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
import com.sun.enterprise.deployment.annotation.handlers.*;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.InboundResourceAdapter;
import com.sun.enterprise.deployment.MessageListener;
import com.sun.enterprise.util.LocalStringManagerImpl;

import javax.resource.spi.Activation;
import java.lang.annotation.Annotation;
import java.util.logging.Level;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.apf.*;
import org.glassfish.apf.impl.HandlerProcessingResultImpl;


/**
 * @author Jagadish Ramu
 */
@Service
@AnnotationHandlerFor(Activation.class)
public class ActivationHandler extends AbstractHandler {

    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ActivationHandler.class);
    
    public HandlerProcessingResult processAnnotation(AnnotationInfo element) throws AnnotationProcessorException {
        AnnotatedElementHandler aeHandler = element.getProcessingContext().getHandler();
        Activation activation = (Activation) element.getAnnotation();
        if (aeHandler instanceof RarBundleContext) {
            RarBundleContext rarContext = (RarBundleContext) aeHandler;
            ConnectorDescriptor desc = rarContext.getDescriptor();

            //process annotation only if message-listeners are provided
            if (activation.messageListeners().length > 0) {
                //initialize inbound if it was not done already
                if (!desc.getInBoundDefined()) {
                    desc.setInboundResourceAdapter(new InboundResourceAdapter());
                }

                InboundResourceAdapter ira = desc.getInboundResourceAdapter();
                
                //get the activation-spec implementation class-name
                Class c = (Class) element.getAnnotatedElement();
                String activationSpecClass = c.getName();

                //process all message-listeners, ensure that no duplicate message-listener-types are found
                for (Class mlClass : activation.messageListeners()) {
                    MessageListener ml = new MessageListener();
                    ml.setActivationSpecClass(activationSpecClass);
                    ml.setMessageListenerType(mlClass.getName());

                    if (!ira.hasMessageListenerType(mlClass.getName())) {
                        ira.addMessageListener(ml);
                    }// else {
                        // ignore the duplicates
                        // duplicates can be via :
                        // (i) message listner defined in DD
                        // (ii) as part of this particular annotation processing,
                        // already this message-listener-type is defined
                        //TODO V3 how to handle (ii)
                    //}
                }
            }
        } else {
            getFailureResult(element, "Not a rar bundle context", true);
        }
        return getDefaultProcessedResult();
    }

    public Class<? extends Annotation>[] getTypeDependencies() {
        return null;
    }

    /**
     * @return a default processed result
     */
    protected HandlerProcessingResult getDefaultProcessedResult() {
        return HandlerProcessingResultImpl.getDefaultResult(
                getAnnotationType(), ResultType.PROCESSED);
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
