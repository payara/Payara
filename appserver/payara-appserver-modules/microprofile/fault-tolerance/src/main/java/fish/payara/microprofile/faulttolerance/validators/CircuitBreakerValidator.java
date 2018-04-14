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
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

/**
 * Validator for the Fault Tolerance CircuitBreaker annotation.
 * @author Andrew Pielage
 */
public class CircuitBreakerValidator {
    
    /**
     * Validates the given CircuitBreaker annotation.
     * @param circuitBreaker The annotation to validate
     * @param annotatedMethod The annotated method to validate
     * @param config The config to get any override values from
     */
    public static void validateAnnotation(CircuitBreaker circuitBreaker, AnnotatedMethod<?> annotatedMethod, 
            Config config) {
        long delay = (Long) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class, "delay", annotatedMethod.getJavaMember().getName(), 
                annotatedMethod.getJavaMember().getDeclaringClass().getCanonicalName(), Long.class)
                .orElse(circuitBreaker.delay());
        
        int requestVolumeThreshold = (Integer) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class, "requestVolumeThreshold", annotatedMethod.getJavaMember().getName(), 
                annotatedMethod.getJavaMember().getDeclaringClass().getCanonicalName(), Integer.class)
                .orElse(circuitBreaker.requestVolumeThreshold());
        
        double failureRatio = (Double) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class, "failureRatio", annotatedMethod.getJavaMember().getName(), 
                annotatedMethod.getJavaMember().getDeclaringClass().getCanonicalName(), Double.class)
                .orElse(circuitBreaker.failureRatio());
        
        int successThreshold = (Integer) FaultToleranceCdiUtils.getOverrideValue(
                config, CircuitBreaker.class, "successThreshold", annotatedMethod.getJavaMember().getName(), 
                annotatedMethod.getJavaMember().getDeclaringClass().getCanonicalName(), Integer.class)
                .orElse(circuitBreaker.successThreshold());
        
        if (delay < 0) {
            throw new FaultToleranceDefinitionException("Method \"" + annotatedMethod.getJavaMember().getName() + "\""
                    + " annotated with " + CircuitBreaker.class.getCanonicalName() 
                    + " has a delay value less than 0: " + delay);
        }
        
        if (requestVolumeThreshold < 1) {
            throw new FaultToleranceDefinitionException("Method \"" + annotatedMethod.getJavaMember().getName() + "\""
                    + " annotated with " + CircuitBreaker.class.getCanonicalName() 
                    + " has a requestVolumeThreshold value less than 1: " + requestVolumeThreshold);
        }
        
        if (failureRatio < 0) {
            throw new FaultToleranceDefinitionException("Method \"" + annotatedMethod.getJavaMember().getName() + "\""
                    + " annotated with " + CircuitBreaker.class.getCanonicalName() 
                    + " has a failureRatio value less than 0: " + failureRatio);
        }
        
        if (failureRatio > 1) {
            throw new FaultToleranceDefinitionException("Method \"" + annotatedMethod.getJavaMember().getName() + "\""
                    + " annotated with " + CircuitBreaker.class.getCanonicalName() 
                    + " has a failureRatio value greater than 1: " + failureRatio);
        }
        
        if (successThreshold < 1) {
            throw new FaultToleranceDefinitionException("Method \"" + annotatedMethod.getJavaMember().getName() + "\""
                    + " annotated with " + CircuitBreaker.class.getCanonicalName() 
                    + " has a successThreshold value less than 1: " + successThreshold);
        }
    }
}
