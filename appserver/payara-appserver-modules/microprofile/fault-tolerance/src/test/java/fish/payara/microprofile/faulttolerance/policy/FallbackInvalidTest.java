/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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

import static fish.payara.microprofile.faulttolerance.test.TestUtils.assertAnnotationInvalid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
import org.junit.Test;

import fish.payara.microprofile.faulttolerance.test.TestUtils;

/**
 * Tests scenarios for invalid declarations as given in the TCK
 * {@code org.eclipse.microprofile.fault.tolerance.tck.fallbackmethod} and
 * {@code org.eclipse.microprofile.fault.tolerance.tck.illegalConfig} package. Names used are based on the TCK tests
 * names.
 * 
 * Test and annotated method are linked by naming conventions. Annotated method and its fallback method are linked by
 * the {@link Fallback} annotation. All names are unique.
 * 
 * These tests can not be run in TCK since they expect the {@link FaultToleranceDefinitionException} to be thrown
 * unwrapped during bootstrap while weld does wrap these when thrown failing the TCK tests.
 *
 * @author Jan Bernitt
 */
@SuppressWarnings("unused")
public class FallbackInvalidTest extends FallbackMethodBeanB<Long> {

    /*
     * FallbackMethodOutOfPackageTest
     */

    @Test
    public void fallbackMethodOutOfPackage() {
        assertAnnotationInvalid("Fallback has a fallbackMethod value referring to a method that is not accessible.");
    }

    @Fallback(fallbackMethod = "fallbackMethodOutOfPackage_Fallback")
    public String fallbackMethodOutOfPackage_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodOutOfPackage");
    }

    /*
     * FallbackMethodSubclassTest
     */

    @Test
    public void fallbackMethodSubclass() {
        assertAnnotationInvalid("Fallback has a fallbackMethod value referring to a method that is not defined or has a incompatible method signature.");
    }

    public String fallbackMethodSubclass_Fallback(int a, Long b) {
        return "fallbackMethodSubclass";
    }

    /*
     * FallbackMethodSuperclassPrivateTest
     */

    @Test
    public void fallbackMethodSuperclassPrivate() {
        assertAnnotationInvalid("Fallback has a fallbackMethod value referring to a method that is not accessible.");
    }

    @Fallback(fallbackMethod = "fallbackMethodSuperclassPrivate_Fallback")
    public String fallbackMethodSuperclassPrivate_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodSuperclassPrivate");
    }

    /*
     * FallbackMethodWildcardNegativeTest
     */

    @Test
    public void fallbackMethodWildcardNegative() {
        assertAnnotationInvalid("Fallback has a fallbackMethod value referring to a method that is not defined or has a incompatible method signature.");
    }

    @Fallback(fallbackMethod = "fallbackMethodWildcardNegative_Fallback")
    public String fallbackMethodWildcardNegative_Method(List<? extends Number> a) {
        throw new RuntimeException("fallbackMethodWildcardNegative");
    }

    public String fallbackMethodWildcardNegative_Fallback(List<? extends Integer> b) {
        return "fallbackMethodWildcardNegative";
    }

    /*
     * IncompatibleFallbackMethodTest
     */

    @Test
    public void fallbackMethodInvalidReturnType() {
        assertAnnotationInvalid("Fallback has a fallbackMethod value whose return type of does not match.");
    }

    @Fallback(fallbackMethod = "fallbackMethodInvalidReturnType")
    public Integer fallbackMethodInvalidReturnType_Method() {
        return 42;
    }

    /**
     * Fallback method with incompatible signature, different return type
     * @return dummy string
     */
    public String fallbackMethodInvalidReturnType_Fallback() {
        return "fallbackMethodInvalidReturnType";
    }

    /*
     * IncompatibleFallbackMethodWithArgsTest
     */

    @Test
    public void fallbackMethodIncompatibleArgumentList() {
        assertAnnotationInvalid("Fallback has a fallbackMethod value referring to a method that is not defined or has a incompatible method signature.");
    }

    @Retry(maxRetries = 4)
    @Fallback(fallbackMethod = "fallbackMethodIncompatibleArgumentList_Fallback")
    public Integer fallbackMethodIncompatibleArgumentList_Method(String name, Integer type) {
        return 42;
    }

    /**
     * Fallback method with incompatible signature, only one parameter
     */
    public Integer fallbackMethodIncompatibleArgumentList_Fallback(String name) {
        return 42;
    }

    /*
     * IncompatibleFallbackPolicies
     */

    public static class IncompatibleFallbackHandler implements FallbackHandler<String> {

        @Override
        public String handle(ExecutionContext context) {
            return "fourty-two";
        }

    }

    @Test
    public void fallbackMethodAndHandlerDefined() {
        assertAnnotationInvalid("Fallback defined both a fallback handler and a fallback method.");
    }

    @Fallback(value = IncompatibleFallbackHandler.class, fallbackMethod = "fallbackMethodAndHandlerDefined_Fallback")
    public int fallbackMethodAndHandlerDefined_Method() {
        return 42;
    }

    public int fallbackMethodAndHandlerDefined_Fallback() {
        return 22;
    }

    /*
     * IncompatibleFallbackTest
     */

    @Test
    public void fallbackHandlerIncompatibleReturnType() {
        assertAnnotationInvalid("Fallback has a value whose return type of does not match.");
    }

    @Fallback(IncompatibleFallbackHandler.class)
    public int fallbackHandlerIncompatibleReturnType_Method() {
        return 42;
    }

}
