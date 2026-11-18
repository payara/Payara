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
// Portions Copyright [2020-2026] Payara Foundation and/or affiliates
package org.glassfish.ejb.deployment.annotation.handlers;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Stateless;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.AnnotationValues;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.ScheduledTimerDescriptor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jvnet.hk2.annotations.Service;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.logging.Level;

/**
 * This handler is responsible for handling the jakarta.ejb.Schedule annotation on methods of a Bean class.
 *
 * @author Marina Vatkina
 */
@Service
@AnnotationHandlerFor(Schedule.class)
public class ScheduleHandler extends AbstractAttributeHandler {

    public ScheduleHandler() {
    }

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            EjbContext[] ejbContexts) throws AnnotationProcessorException {

        return processSchedule(ainfo.getAnnotation(), ainfo, ejbContexts);
    }

    protected HandlerProcessingResult processSchedule(AnnotationInstance sch,
            AnnotationInfo ainfo, EjbContext[] ejbContexts)
            throws AnnotationProcessorException {
        IndexView index = ainfo.getIndex();

        for (EjbContext ejbContext : ejbContexts) {
            EjbDescriptor ejbDesc = (EjbDescriptor) ejbContext.getDescriptor();

            if (ElementType.METHOD.equals(ainfo.getElementType())) {
                MethodInfo annMethod = ainfo.getAnnotatedElement().asMethod();

                // Only add the method-level timer schedule if the method doesn't
                // already have a schedule from the deployment descriptor.
                if( !ejbDesc.hasScheduledTimerMethodFromDD(annMethod)) {
                    ScheduledTimerDescriptor sd = new ScheduledTimerDescriptor();
                    sd.setSecond(AnnotationValues.string(index, sch, "second"));
                    sd.setMinute(AnnotationValues.string(index, sch, "minute"));
                    sd.setHour(AnnotationValues.string(index, sch, "hour"));
                    sd.setDayOfMonth(AnnotationValues.string(index, sch, "dayOfMonth"));
                    sd.setMonth(AnnotationValues.string(index, sch, "month"));
                    sd.setDayOfWeek(AnnotationValues.string(index, sch, "dayOfWeek"));
                    sd.setYear(AnnotationValues.string(index, sch, "year"));
                    sd.setTimezone(AnnotationValues.string(index, sch, "timezone"));
                    sd.setPersistent(AnnotationValues.bool(index, sch, "persistent"));
                    sd.setInfo(AnnotationValues.string(index, sch, "info"));
                    sd.setTimeoutMethod(new MethodDescriptor(annMethod));

                    ejbDesc.addScheduledTimerDescriptor(sd);

                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "@@@ Found Schedule on {0}", annMethod);

                        logger.log(Level.FINE, "@@@ TimerConfig : {0} # {1}",
                                new Object[]{(sd.getInfo() != null && !sd.getInfo().equals(""))? sd.getInfo() : null, sd.getPersistent()});
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
    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Annotation>[] getTypeDependencies() {
        return new Class[] {Stateless.class, Singleton.class, MessageDriven.class};
    }

    @Override
    protected boolean supportTypeInheritance() {
        return true;
    }
}
