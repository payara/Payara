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

package org.glassfish.ejb.mdb.deployment.annotation.handlers;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.logging.Level;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

import org.glassfish.ejb.deployment.annotation.handlers.AbstractEjbHandler;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.annotation.context.EjbBundleContext;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbMessageBeanDescriptor;
import org.jvnet.hk2.annotations.Service;

/**
 * This handler is responsible for handling the javax.ejb.MessageDriven
 *
 * @author Shing Wai Chan
 */
@Service
@AnnotationHandlerFor(MessageDriven.class)
public class MessageDrivenHandler extends AbstractEjbHandler {
    
    /** Creates a new instance of MessageDrivenHandler */
    public MessageDrivenHandler() {
    }
    
    /**
     * Return the name attribute of given annotation.
     * @param annotation
     * @return name
     */
    protected String getAnnotatedName(Annotation annotation) {
        MessageDriven mdAn = (MessageDriven)annotation;
        return mdAn.name();
    }

    /**
     * Check if the given EjbDescriptor matches the given Annotation.
     * @param ejbDesc
     * @param annotation
     * @return boolean check for validity of EjbDescriptor
     */
    protected boolean isValidEjbDescriptor(EjbDescriptor ejbDesc,
            Annotation annotation) {
        return EjbMessageBeanDescriptor.TYPE.equals(ejbDesc.getType());
    }

    /**
     * Create a new EjbDescriptor for a given elementName and AnnotationInfo.
     * @param elementName
     * @param ainfo
     * @return a new EjbDescriptor
     */
    protected EjbDescriptor createEjbDescriptor(String elementName,
            AnnotationInfo ainfo) throws AnnotationProcessorException {

        AnnotatedElement ae = ainfo.getAnnotatedElement();
        EjbMessageBeanDescriptor newDescriptor = new EjbMessageBeanDescriptor();
        Class ejbClass = (Class)ae;
        newDescriptor.setName(elementName);
        newDescriptor.setEjbClassName(ejbClass.getName());
        return newDescriptor;
    }

    /**
     * Set Annotation information to Descriptor.
     * This method will also be invoked for an existing descriptor with
     * annotation as user may not specific a complete xml.
     * @param ejbDesc
     * @param ainfo
     * @return HandlerProcessingResult
     */
    protected HandlerProcessingResult setEjbDescriptorInfo(
            EjbDescriptor ejbDesc, AnnotationInfo ainfo)
            throws AnnotationProcessorException {

        MessageDriven mdAn = (MessageDriven)ainfo.getAnnotation();
        Class ejbClass = (Class)ainfo.getAnnotatedElement();
        EjbMessageBeanDescriptor ejbMsgBeanDesc =
                (EjbMessageBeanDescriptor)ejbDesc;
   
        HandlerProcessingResult procResult = 
            setMessageListenerInterface(
                    mdAn, ejbMsgBeanDesc, ejbClass, ainfo);

        doDescriptionProcessing(mdAn.description(), ejbMsgBeanDesc);
        doMappedNameProcessing(mdAn.mappedName(), ejbMsgBeanDesc);

        for (ActivationConfigProperty acProp : mdAn.activationConfig()) {
            EnvironmentProperty envProp = new EnvironmentProperty(
                    acProp.propertyName(), acProp.propertyValue(), "");
                                                // with empty description
            // xml override
            if (ejbMsgBeanDesc.getActivationConfigValue(envProp.getName()) == null) {
                ejbMsgBeanDesc.putActivationConfigProperty(envProp);
            }
        }

        return procResult;
    }

    private HandlerProcessingResult setMessageListenerInterface(
            MessageDriven mdAn, EjbMessageBeanDescriptor msgEjbDesc,
            Class ejbClass, AnnotationInfo ainfo)
            throws AnnotationProcessorException {

        String intfName = null;

        // If @MessageDriven contains message listener interface, that takes
        // precedence.  Otherwise, the message listener interface is derived
        // from the implements clause.  

        if( mdAn.messageListenerInterface() != Object.class ) {
            intfName = mdAn.messageListenerInterface().getName();
        } else {
            for(Class next : ejbClass.getInterfaces()) {
                if( !excludedFromImplementsClause(next) ) {
                    if( intfName == null ) {
                        intfName = next.getName();
                    } else {
                        EjbBundleDescriptorImpl currentBundle = (EjbBundleDescriptorImpl)
                        ((EjbBundleContext)ainfo.getProcessingContext().getHandler()).getDescriptor();
                        log(Level.SEVERE, ainfo, 
                            localStrings.getLocalString(
                            "enterprise.deployment.annotation.handlers.ambiguousimplementsclausemdb",
                            "Implements clause for 3.x message driven bean class {0} in {1} declares more than one potential message-listener interface.  In this case, the @MessageDriven.messageListenerInterface() attribute must be used to specify the message listener interface.",
                             new Object[] { ejbClass,
                             currentBundle.getModuleDescriptor().getArchiveUri() }));
                        return getDefaultFailedResult();
                    }
                }
            }
        }

        // if it's still null, check whether it's defined through
        // deployment descriptor
        // note: the descriptor class has a default value 
        // for the interface: javax.jms.MessageListener
        // so intfName after this set, will never be null
        if (intfName == null) {
            intfName = msgEjbDesc.getMessageListenerType();
        }

        msgEjbDesc.setMessageListenerType(intfName);

        return getDefaultProcessedResult();
    }
}
