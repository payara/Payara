/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.microprofile.faulttolerance.policy;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.junit.Test;

import fish.payara.microprofile.faulttolerance.service.FaultToleranceServiceStub;
import fish.payara.microprofile.faulttolerance.test.TestUtils;

/**
 * Tests the basic correctness of the {@link Fallback} handling.
 * 
 * @author Jan Bernitt
 */
public class FallbackBasicTest implements FallbackHandler<String> {

    /**
     * Most basic case where annotated method fails and the fallback returns a result (no method arguments)
     */
    @Test
    public void fallbackMethod( ) throws Exception {
        assertEquals("fallbackMethod", proceedToResultValue());
    }

    @Fallback(fallbackMethod = "fallbackMethod_FallbackMethod")
    public String fallbackMethod_Method() {
        throw new RuntimeException("Normal execution failed.");
    }

    public String fallbackMethod_FallbackMethod() {
        return "fallbackMethod";
    }

    /**
     * Annotated method fails and fallback returns a result with method arguments
     */
    @Test
    public void fallbackMethodWithArguments() throws Exception {
        assertEquals("fallbackMethodWithArguments:Peter", proceedToResultValue("Peter"));
    }

    @Fallback(fallbackMethod = "fallbackMethodWithArguments_FallbackMethod")
    public String fallbackMethodWithArguments_Method(@SuppressWarnings("unused") String name) {
        throw new RuntimeException("Normal execution failed.");
    }

    public String fallbackMethodWithArguments_FallbackMethod(String name) {
        return "fallbackMethodWithArguments:" + name;
    }

    /**
     * Both annotated method and fallback method fail.
     */
    @Test(expected = IllegalStateException.class)
    public void fallbackMethodFailsToo() throws Exception {
        proceedToResultValue();
    }

    @Fallback(fallbackMethod = "fallbackMethodFailsToo_FallbackMethod")
    public void fallbackMethodFailsToo_Method() {
        throw new RuntimeException("Normal execution failed.");
    }

    public void fallbackMethodFailsToo_FallbackMethod() {
        throw new IllegalStateException("Fallback fails as well.");
    }

    /**
     * Most basic {@link FallbackHandler} test where annotated method fails and fallback returns a result value. 
     */
    @Test
    public void fallbackHandler() throws Exception {
        assertEquals("fallbackHandler", proceedToResultValue());
    }

    @Fallback(value = FallbackBasicTest.class)
    public String fallbackHandler_Method() {
        throw new RuntimeException("Normal execution failed.");
    }

    /**
     * Basic case of a {@link FallbackHandler} for a method with arguments.
     */
    @Test
    public void fallbackHandlerWithArguments() throws Exception {
        assertEquals("fallbackHandlerWithArguments:Peter", proceedToResultValue("Peter"));
    }

    @Fallback(value = FallbackBasicTest.class)
    public String fallbackHandlerWithArguments_Method(@SuppressWarnings("unused") String name) {
        throw new RuntimeException("Normal execution failed.");
    }

    /**
     * Both annotated method and fallback handler fail.
     */
    @Test(expected = IllegalStateException.class)
    public void fallbackHandlerFailsToo() throws Exception {
        proceedToResultValue();
    }

    @Fallback(value = FallbackBasicTest.class)
    public String fallbackHandlerFailsToo_Method() {
        throw new RuntimeException("Normal execution failed.");
    }

    /**
     * Implements the result for all 3 {@link FallbackHandler} tests.
     */
    @Override
    public String handle(ExecutionContext context) {
        if (context.getMethod().getName().equals("fallbackHandlerFailsToo_Method")) {
            throw new IllegalStateException("Handler fails as well");
        }
        String resultValue = context.getMethod().getName().replace("_Method", "");
        if (context.getParameters().length > 0) {
            resultValue += ":" + context.getParameters()[0];
        }
        return resultValue;
    }

    private Object proceedToResultValue(Object... methodArguments) throws Exception {
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        FaultTolerancePolicy policy = FaultTolerancePolicy.asAnnotated(getClass(), annotatedMethod);
        StaticAnalysisContext context = new StaticAnalysisContext(this, annotatedMethod, methodArguments);
        return policy.proceed(context, () -> new FaultToleranceServiceStub().getMethodContext(context, policy));
    }
}
