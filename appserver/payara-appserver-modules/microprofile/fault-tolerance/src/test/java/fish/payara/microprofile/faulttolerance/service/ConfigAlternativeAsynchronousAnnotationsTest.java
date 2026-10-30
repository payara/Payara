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
package fish.payara.microprofile.faulttolerance.service;

import static fish.payara.microprofile.faulttolerance.FaultToleranceConfig.ALTERNATIVE_ASYNCHRONOUS_ANNNOTATIONS_PROPERTY;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.junit.Test;

import fish.payara.microprofile.faulttolerance.FaultToleranceConfig;
import fish.payara.microprofile.faulttolerance.policy.FaultTolerancePolicy;
import fish.payara.microprofile.faulttolerance.policy.StaticAnalysisContext;
import fish.payara.microprofile.faulttolerance.test.ConfigOverrides;
import fish.payara.microprofile.faulttolerance.test.TestUtils;

/**
 * Tests that {@link FaultToleranceConfig#ALTERNATIVE_ASYNCHRONOUS_ANNNOTATIONS_PROPERTY} can be used to configure
 * alternative annotations that can be used to get the {@link Asynchronous} behaviour.
 * 
 * @author Jan Bernitt
 */
public class ConfigAlternativeAsynchronousAnnotationsTest {

    @Target({METHOD, TYPE})
    @Retention(RUNTIME)
    @interface OurAsynchronous {
        // a user defined alternative Asynchronous annotation
    }

    private ConfigOverrides config = new ConfigOverrides();
    private BindableFaultToleranceConfig configFactory = new BindableFaultToleranceConfig(config, null);

    /**
     * Test with a single alternative annotation of type {@link jakarta.ejb.Asynchronous} set.
     */
    @Test
    public void javaxEjbAsynchronous() {
        config.override(ALTERNATIVE_ASYNCHRONOUS_ANNNOTATIONS_PROPERTY, jakarta.ejb.Asynchronous.class.getName());
        FaultTolerancePolicy policy = getPolicy();
        assertNotNull(policy.asynchronous);
        assertTrue("Should be FUTURE", policy.asynchronous.isSuccessWhenCompletedExceptionally());
    }

    @jakarta.ejb.Asynchronous
    public Future<String> javaxEjbAsynchronous_Method() {
        return null;
    }

    /**
     * Tests with two alternative annotations, one of which is a user defined one {@link OurAsynchronous}.
     */
    @Test
    public void userDefinedAsynchronousAnnotation() {
        config.override(ALTERNATIVE_ASYNCHRONOUS_ANNNOTATIONS_PROPERTY, 
                jakarta.ejb.Asynchronous.class.getName() + "," + OurAsynchronous.class.getName());
        FaultTolerancePolicy policy = getPolicy();
        assertNotNull(policy.asynchronous);
        assertFalse("Should be COMPLETION_STAGE", policy.asynchronous.isSuccessWhenCompletedExceptionally());
    }

    @OurAsynchronous
    public CompletionStage<String> userDefinedAsynchronousAnnotation_Method() {
        return null;
    }

    /**
     * Again two alternative annotations are set but the FT {@link Asynchronous} annotation is used and should still
     * work (even when not in the list configured).
     */
    @Test
    public void asynchronousStillRecognisedWhenSettingAlternativeAnnotations() {
        config.override(ALTERNATIVE_ASYNCHRONOUS_ANNNOTATIONS_PROPERTY, 
                jakarta.ejb.Asynchronous.class.getName() + "," + OurAsynchronous.class.getName());
        FaultTolerancePolicy policy = getPolicy();
        assertNotNull(policy.asynchronous);
        assertTrue("Should be FUTURE", policy.asynchronous.isSuccessWhenCompletedExceptionally());
    }

    @Asynchronous
    public Future<String> asynchronousStillRecognisedWhenSettingAlternativeAnnotations_Method() {
        return null;
    }

    private FaultTolerancePolicy getPolicy() {
        Method annotatedMethod = TestUtils.getAnnotatedMethod();
        StaticAnalysisContext context = new StaticAnalysisContext(getClass(), annotatedMethod);
        return FaultTolerancePolicy.get(context, () -> configFactory.bindTo(context));
    }
}
