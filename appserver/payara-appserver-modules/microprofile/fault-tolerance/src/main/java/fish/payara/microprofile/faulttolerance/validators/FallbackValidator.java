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

import static fish.payara.microprofile.faulttolerance.FaultToleranceService.FALLBACK_HANDLER_METHOD_NAME;
import fish.payara.microprofile.faulttolerance.cdi.FaultToleranceCdiUtils;
import javax.enterprise.inject.spi.AnnotatedMethod;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

/**
 * Validator for the Fault Tolerance Fallback Annotation.
 * @author Andrew Pielage
 */
public class FallbackValidator {
    
    /**
     * Validate that the Fallback annotation is correct.
     * @param fallback The fallback annotation to validate
     * @param annotatedMethod The method annotated with @Fallback
     * @param config The config of the application
     * @throws ClassNotFoundException If the fallbackClass could not be found
     * @throws NoSuchMethodException If the fallbackMethod could not be found
     */
    public static void validateAnnotation(Fallback fallback, AnnotatedMethod<?> annotatedMethod, Config config) 
            throws ClassNotFoundException, NoSuchMethodException {
        // Get the fallbackMethod
        String fallbackMethod = (String) FaultToleranceCdiUtils.getOverrideValue(
                config, Fallback.class, "fallbackMethod", annotatedMethod.getJavaMember().getName(), 
                annotatedMethod.getJavaMember().getDeclaringClass().getCanonicalName(), String.class)
                .orElse(fallback.fallbackMethod());
        
        // Get the fallbackClass, and check that it can be found
        Class<? extends FallbackHandler> fallbackClass = (Class<? extends FallbackHandler>) Thread.currentThread()
                .getContextClassLoader().loadClass((String) FaultToleranceCdiUtils
                .getOverrideValue(config, Fallback.class, "value", annotatedMethod.getJavaMember().getName(), 
                annotatedMethod.getJavaMember().getDeclaringClass().getCanonicalName(), String.class)
                .orElse(fallback.value().getName()));
        
        // Validate the annotated method
        if (fallbackMethod != null && !fallbackMethod.isEmpty()) {
            if (fallbackClass != null && fallbackClass != Fallback.DEFAULT.class) {
                throw new FaultToleranceDefinitionException("Both a fallback class and method have been set.");
            } else {
                try {
                    if (annotatedMethod.getJavaMember().getDeclaringClass().getDeclaredMethod(fallbackMethod, 
                            annotatedMethod.getJavaMember().getParameterTypes()).getReturnType() 
                            != annotatedMethod.getJavaMember().getReturnType()) {
                        throw new FaultToleranceDefinitionException("Return type of fallback method does not match.");
                    }
                } catch (NoSuchMethodException ex) {
                    throw new FaultToleranceDefinitionException("Could not find fallback method: " + fallbackMethod, ex);
                }
            }
        } else if (fallbackClass != null && fallbackClass != Fallback.DEFAULT.class) {
            if (fallbackClass.getDeclaredMethod(FALLBACK_HANDLER_METHOD_NAME, ExecutionContext.class).getReturnType() 
                    != annotatedMethod.getJavaMember().getReturnType()) {
                throw new FaultToleranceDefinitionException(
                        "Return type of fallback class handle method does not match.");
            }
        }
    }
}
