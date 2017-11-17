/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.microprofile.faulttolerance.validators;

import fish.payara.microprofile.faulttolerance.cdi.FaultToleranceCdiUtils;
import javax.enterprise.inject.spi.AnnotatedMethod;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

/**
 * Validator for the Fault Tolerance Retry annotation.
 * @author Andrew Pielage
 */
public class RetryValidator {
    
    /**
     * Validates the given Retry annotation.
     * @param retry The annotation to validate
     * @param annotatedMethod The annotated method to validate
     * @param config The config to get any override values from
     */
    public static void validateAnnotation(Retry retry, AnnotatedMethod<?> annotatedMethod, Config config) {
        int maxRetries = (Integer) FaultToleranceCdiUtils.getOverrideValue(
                config, Retry.class, "maxRetries", annotatedMethod.getJavaMember().getName(), 
                annotatedMethod.getJavaMember().getDeclaringClass().getCanonicalName(), Integer.class)
                .orElse(retry.maxRetries());
        
        long delay = (Long) FaultToleranceCdiUtils.getOverrideValue(
                config, Retry.class, "delay", annotatedMethod.getJavaMember().getName(), 
                annotatedMethod.getJavaMember().getDeclaringClass().getCanonicalName(), Long.class)
                .orElse(retry.delay());
        
        long maxDuration = (Long) FaultToleranceCdiUtils.getOverrideValue(
                config, Retry.class, "maxDuration", annotatedMethod.getJavaMember().getName(), 
                annotatedMethod.getJavaMember().getDeclaringClass().getCanonicalName(), Long.class)
                .orElse(retry.maxDuration());
        
        long jitter = (Long) FaultToleranceCdiUtils.getOverrideValue(
                config, Retry.class, "jitter", annotatedMethod.getJavaMember().getName(), 
                annotatedMethod.getJavaMember().getDeclaringClass().getCanonicalName(), Long.class)
                .orElse(retry.jitter());
        
        if (maxRetries < -1) {
            throw new FaultToleranceDefinitionException("Method \"" + annotatedMethod.getJavaMember().getName() + "\""
                    + " annotated with " + Retry.class.getCanonicalName() 
                    + " has a maxRetries value less than -1: " + maxRetries);
        }
        
        if (delay < 0) {
            throw new FaultToleranceDefinitionException("Method \"" + annotatedMethod.getJavaMember().getName() + "\""
                    + " annotated with " + Retry.class.getCanonicalName() 
                    + " has a delay value less than 0: " + delay);
        }
        
        if (maxDuration < 0) {
            throw new FaultToleranceDefinitionException("Method \"" + annotatedMethod.getJavaMember().getName() + "\""
                    + " annotated with " + Retry.class.getCanonicalName() 
                    + " has a maxDuration value less than 0: " + maxDuration);
        }
        
        if (maxDuration <= delay) {
            throw new FaultToleranceDefinitionException("Method \"" + annotatedMethod.getJavaMember().getName() + "\""
                    + " annotated with " + Retry.class.getCanonicalName() 
                    + " has a maxDuration value less than or equal to the delay value: " + maxDuration);
        }
        
        if (jitter < 0) {
            throw new FaultToleranceDefinitionException("Method \"" + annotatedMethod.getJavaMember().getName() + "\""
                    + " annotated with " + Retry.class.getCanonicalName() 
                    + " has a jitter value less than 0: " + jitter);
        }
    }
}
