/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.deployment.annotation.handlers;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import java.util.logging.Level;

import javax.ejb.*;

import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.ScheduledTimerDescriptor;
//import com.sun.ejb.containers.TimerSchedule;

import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import org.glassfish.ejb.deployment.annotation.handlers.AbstractAttributeHandler;
import org.jvnet.hk2.annotations.Service;

/**
 * This handler is responsible for handling the javax.ejb.Schedule
 * annotation on methods of a Bean class. 
 *
 * @author Marina Vatkina
 */
@Service
public class ScheduleHandler extends AbstractAttributeHandler {

    public ScheduleHandler() {
    }
    
    /**
     * @return the annoation type this annotation handler is handling
     */
    public Class<? extends Annotation> getAnnotationType() {
        return Schedule.class;
    }    
        
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            EjbContext[] ejbContexts) throws AnnotationProcessorException {

        return processSchedule((Schedule)ainfo.getAnnotation(), ainfo, ejbContexts);
    }
        
    protected HandlerProcessingResult processSchedule(Schedule sch,
            AnnotationInfo ainfo, EjbContext[] ejbContexts) 
            throws AnnotationProcessorException {

        for (EjbContext ejbContext : ejbContexts) {
            EjbDescriptor ejbDesc = ejbContext.getDescriptor();

            if (ElementType.METHOD.equals(ainfo.getElementType())) {
                Method annMethod = (Method) ainfo.getAnnotatedElement();

                // .xml-defined timer method overrides @Schedule
                if( !ejbDesc.hasScheduledTimerMethodFromDD(annMethod)) {
                    ScheduledTimerDescriptor sd = new ScheduledTimerDescriptor();
                    sd.setSecond(sch.second());
                    sd.setMinute(sch.minute());
                    sd.setHour(sch.hour());
                    sd.setDayOfMonth(sch.dayOfMonth());
                    sd.setMonth(sch.month());
                    sd.setDayOfWeek(sch.dayOfWeek());
                    sd.setYear(sch.year());
                    sd.setTimezone(sch.timezone());
                    sd.setPersistent(sch.persistent());
                    sd.setInfo(sch.info());
                    sd.setTimeoutMethod(new MethodDescriptor(annMethod));

                    ejbDesc.addScheduledTimerDescriptor(sd);

                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("@@@ Found Schedule on " + annMethod);
                        
                        /* Issue 17038, commented out for now since it references 
                         * TimerSchedule class in ejb-container module.
                        TimerSchedule ts = new TimerSchedule(sd, 
                                annMethod.getName(), annMethod.getParameterTypes().length);

                        java.util.Calendar date = ts.getNextTimeout();
                        logger.fine("@@@ First timeout: " + 
                                ((ts.isValid(date))? date.getTime() : "NEVER"));
                        logger.fine("@@@ Schedule : " + ts.getScheduleAsString());
                         
                         */
                        logger.fine("@@@ TimerConfig : " + 
                                ((sd.getInfo() != null && !sd.getInfo().equals(""))? sd.getInfo() : null) + 
                                " # " + sd.getPersistent());
                    }
                }
            }
        }

        return getDefaultProcessedResult();
    }

    /**
     * @return an array of annotation types this annotation handler would 
     * require to be processed (if present) before it processes it's own 
     * annotation type.
     */
    public Class<? extends Annotation>[] getTypeDependencies() {
        
        return new Class[] {Stateless.class, Singleton.class, MessageDriven.class};
                
    }

    protected boolean supportTypeInheritance() {
        return true;
    }
}
