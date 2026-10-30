/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2025] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.micro.cdi.extension.cluster;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.enterprise.inject.spi.Bean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import fish.payara.cluster.Clustered;
import org.mockito.MockitoAnnotations;

/**
 * @author Matt Gill
 */
public class ClusterScopeContextTest {

    private static final String ANNOTATION_BEAN_NAME = "Annotation";
    private static final String CDI_BEAN_NAME = "CDI";
    private static final Class BEAN_CLASS = Object.class;

    @Mock
    private Bean<?> bean;

    @Mock
    private Clustered annotation;

    @Before
    public void initialiseMocks() {
        MockitoAnnotations.initMocks(this);
        when(annotation.keyName()).thenReturn(ANNOTATION_BEAN_NAME);
        when(bean.getName()).thenReturn(CDI_BEAN_NAME);
        when(bean.getBeanClass()).thenReturn(BEAN_CLASS);
    }

    @Test
    public void when_key_name_not_empty_expect_correct_getBeanName() {
        assertEquals(ANNOTATION_BEAN_NAME, ClusterScopeContext.getBeanName(bean, annotation));
    }

    @Test
    public void when_key_name_null_expect_correct_getBeanName() {
        when(annotation.keyName()).thenReturn(null);
        assertEquals(CDI_BEAN_NAME, ClusterScopeContext.getBeanName(bean, annotation));
    }

    @Test
    public void when_key_name_empty_expect_correct_getBeanName() {
        when(annotation.keyName()).thenReturn("");
        assertEquals(CDI_BEAN_NAME, ClusterScopeContext.getBeanName(bean, annotation));
    }

    @Test
    public void when_key_name_blank_expect_correct_getBeanName() {
        when(annotation.keyName()).thenReturn(" ");
        assertEquals(CDI_BEAN_NAME, ClusterScopeContext.getBeanName(bean, annotation));
    }

    @Test
    public void when_key_name_and_bean_name_null_expect_correct_getBeanName() {
        when(annotation.keyName()).thenReturn(null);
        when(bean.getName()).thenReturn(null);
        assertEquals("java.lang.Object", ClusterScopeContext.getBeanName(bean, annotation));
    }

}