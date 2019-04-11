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
package fish.payara.microprofile.faulttolerance.interceptors;

import java.io.Serializable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.logging.Level;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;

/**
 * Interceptor for the Fault Tolerance Asynchronous Annotation. Also contains the wrapper class for the Future outcome.
 *
 * @author Andrew Pielage
 */
@Interceptor
@Asynchronous
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class AsynchronousInterceptor extends BaseFaultToleranceInterceptor<Asynchronous> implements Serializable {

    public AsynchronousInterceptor() {
        super(Asynchronous.class, false);
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Object resultValue = null;

        try {
            // Attempt to proceed the InvocationContext with Asynchronous semantics if Fault Tolerance is enabled for 
            // this method
            if (getConfig().isEnabled(context) && getConfig().isEnabled(Asynchronous.class, context)) {
                resultValue = asynchronous(context);
            } else {
                // If fault tolerance isn't enabled, just proceed as normal
                logger.log(Level.FINE, "Fault Tolerance not enabled, proceeding normally without asynchronous.");
                resultValue = context.proceed();
            }
        } catch (Exception ex) {
            // If an exception was thrown, check if the method is annotated with @Fallback
            // We should only get here if executing synchronously, as the exception wouldn't get thrown in this thread
            Fallback fallback = getConfig().getAnnotation(Fallback.class, context);

            // If the method was annotated with Fallback and the annotation is enabled, attempt it, otherwise just 
            // propagate the exception upwards
            if (fallback != null && getConfig().isEnabled(Fallback.class, context)) {
                logger.log(Level.FINE, "Fallback annotation found on method - falling back from Asynchronous");
                //FallbackPolicy fallbackPolicy = new FallbackPolicy(fallback, getConfig(), getExecution(), getMetrics(), context);
                resultValue = null; // fallbackPolicy.fallback(context, ex);
            } else {
                throw ex;
            }
        }

        return resultValue;
    }

    private Object asynchronous(InvocationContext context) throws Exception, NamingException {
        Class<?> returnType = context.getMethod().getReturnType();
        if (returnType == CompletionStage.class) {
            logger.log(Level.FINER, "Proceeding invocation asynchronously");
            //TODO
            return context.proceed();
        } 
        if (returnType == Future.class) {
            logger.log(Level.FINER, "Proceeding invocation asynchronously");
            return null; //TODO run 
        }
        logger.log(Level.SEVERE, "Unsupported return type for @Asynchronous annotated method: " + returnType
                + ", proceeding normally without asynchronous.");
        return context.proceed();
    }
}
