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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
import org.junit.Test;

import fish.payara.microprofile.faulttolerance.test.TestUtils;

/**
 * Tests scenarios for valid declarations as given in the TCK
 * {@code org.eclipse.microprofile.fault.tolerance.tck.fallbackmethod} package. Names used are based on the TCK tests
 * names.
 * 
 * Test and annotated method are linked by naming conventions. Annotated method and its fallback method are linked by
 * the {@link Fallback} annotation. All names are unique.
 * 
 * The fallback method lookup is non-trivial and debugging with the cycle of building, deploying and starting/stopping
 * the app and run TCK simply become to time intensive to work out the correct behaviour.
 * 
 * @author Jan Bernitt
 */
@SuppressWarnings("unused")
public class FallbackMethodTest extends FallbackMethodBeanA<Long, String> {

    /*
     * FallbackMethodAbstractTest
     */

    @Test
    public void fallbackMethodAbstract() {
        assertFallbackMethod();
    }

    @Override
    protected String fallbackMethodAbstract_Fallback(int a, Long b) {
        return "fallbackMethodAbstract";
    }

    /*
     * FallbackMethodBasicTest
     */

    @Test
    public void fallbackMethodBasic() {
        assertFallbackMethod();
    }

    @Fallback(fallbackMethod = "fallbackMethodBasic_Fallback")
    public String fallbackMethodBasic_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodBasic");
    }

    public String fallbackMethodBasic_Fallback(int a, Long b) {
        return "fallbackMethodBasic";
    }

    /*
     * FallbackMethodDefaultMethodTest
     */

    @Test
    public void fallbackMethodDefaultMethod() {
        assertFallbackMethod();
    }

    @Fallback(fallbackMethod = "fallbackMethodDefaultMethod_Fallback")
    public String fallbackMethodDefaultMethod_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodDefaultMethod");
    }

    /*
     * FallbackMethodGenericAbstractTest
     */

    @Test
    public void fallbackMethodGenericAbstract() {
        assertFallbackMethod();
    }

    @Override
    protected String fallbackMethodGenericAbstract_Fallback(int a, Long b) {
        return "fallbackMethodGenericAbstract";
    }

    /*
     * FallbackMethodGenericArrayTest
     */

    @Test
    public void fallbackMethodGenericArray() {
        assertFallbackMethod();
    }

    @Fallback(fallbackMethod = "fallbackMethodGenericArray_Fallback")
    public String fallbackMethodGenericArray_Method(String[][] arg) {
        throw new RuntimeException("fallbackMethodGenericArray");
    }

    /*
     * FallbackMethodGenericComplexTest
     */

    @Test
    public void fallbackMethodGenericComplex() {
        assertFallbackMethod();
    }

    @Fallback(fallbackMethod = "fallbackMethodGenericComplex_Fallback")
    public String fallbackMethodGenericComplex_Method(List<Set<String>> a) {
        throw new RuntimeException("fallbackMethodGenericComplex");
    }

    /*
     * FallbackMethodGenericDeepTest
     */

    @Test
    public void fallbackMethodGenericDeep() {
        assertFallbackMethod();
    }

    @Fallback(fallbackMethod = "fallbackMethodGenericDeep_Fallback")
    public String fallbackMethodGenericDeep_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodGenericDeep");
    }

    /*
     * FallbackMethodGenericTest
     */

    @Test
    public void fallbackMethodGeneric() {
        assertFallbackMethod();
    }

    @Fallback(fallbackMethod = "fallbackMethodGeneric_Fallback")
    public String fallbackMethodGeneric_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodGeneric");
    }

    /*
     * FallbackMethodGenericWildcardTest
     */

    @Test
    public void fallbackMethodGenericWildcard() {
        assertFallbackMethod();
    }

    @Fallback(fallbackMethod = "fallbackMethodGenericWildcard_Fallback")
    public String fallbackMethodGenericWildcard_Method(List<? extends String> a) {
        throw new RuntimeException("fallbackMethodGenericWildcard");
    }

    /*
     * FallbackMethodInPackageTest
     */

    @Test
    public void fallbackMethodInPackage() {
        assertFallbackMethod();
    }

    @Fallback(fallbackMethod = "fallbackMethodInPackage_Fallback")
    public String fallbackMethodInPackage_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodInPackage");
    }

    /*
     * FallbackMethodInterfaceTest
     */

    @Test
    public void fallbackMethodInterface() {
        assertFallbackMethod(FallbackMethodBeanA.class);
    }

    @Override
    public String fallbackMethodInterface_Fallback(int a, Long b) {
        return "fallbackMethodInterface";
    }

    /*
     * FallbackMethodPrivateTest
     */

    @Test
    public void fallbackMethodPrivate() {
        assertFallbackMethod();
    }

    @Fallback(fallbackMethod = "fallbackMethodPrivate_Fallback")
    public String fallbackMethodPrivate_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodPrivate");
    }

    private String fallbackMethodPrivate_Fallback(int a, Long b) {
        return "fallbackMethodPrivate";
    }

    /*
     * FallbackMethodSubclassOverrideTest
     */

    @Test
    public void fallbackMethodSubclassOverride() {
        assertFallbackMethod();
    }

    /*
     * FallbackMethodSuperclassTest
     */

    @Test
    public void fallbackMethodSuperclass() {
        assertFallbackMethod();
    }

    @Fallback(fallbackMethod = "fallbackMethodSuperclass_Fallback")
    public String fallbackMethodSuperclass_Method(int a, Long b) {
        throw new RuntimeException("fallbackMethodSuperclass");
    }

    /*
     * FallbackMethodVarargsTest
     */

    @Test
    public void fallbackMethodVarargs() {
        assertFallbackMethod();
    }

    @Fallback(fallbackMethod = "fallbackMethodVarargs_Fallback")
    public String fallbackMethodVarargs_Method(int a, Long... b) {
        throw new RuntimeException("fallbackMethodVarargs");
    }

    public String fallbackMethodVarargs_Fallback(int a, Long... b) {
        return "fallbackMethodVarargs";
    }

    /*
     * FallbackMethodWildcardTest
     */

    @Test
    public void fallbackMethodWildcard() {
        assertFallbackMethod();
    }

    @Fallback(fallbackMethod = "fallbackMethodWildcard_Fallback")
    public String fallbackMethodWildcard_Method(List<?> a) {
        throw new RuntimeException("fallbackMethodWildcard");
    }

    public String fallbackMethodWildcard_Fallback(List<?> b) {
        return "fallbackMethodWildcard";
    }

    /*
     * Helper Methods
     */

    private void assertFallbackMethod() {
        assertFallbackMethod(getClass());
    }

    private void assertFallbackMethod(Class<?> target) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String testName = stackTraceElements[target == getClass() ? 3 : 2].getMethodName();
        Method annotatedMethod = TestUtils.getMethod(target, testName + "_Method");
        Fallback fallback = annotatedMethod.getAnnotation(Fallback.class);
        FallbackPolicy policy = new FallbackPolicy(annotatedMethod, fallback.value(), fallback.fallbackMethod());
        assertNotNull(policy);
        assertNotNull(policy.fallbackMethod);
        assertNotNull(policy.method);
        try {
            Object[] args = TestUtils.createNullArgumentsFor(annotatedMethod);
            Object actual = policy.method.invoke(this, args);
            assertEquals(testName, actual.toString());
        } catch (Exception e) {
            throw new AssertionError("Failed to invoke fallback method", e);
        }
    }
}
