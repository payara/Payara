/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.config.cdi.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Member;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Test;

/**
 * Unit tests for {@link ConfigPropertyModel}.
 *
 * These tests verify the name derivation and default-value extraction logic in
 * ConfigPropertyModel without requiring a live CDI container. All CDI types are
 * mocked via Mockito.
 *
 * JPMS note: the module-info.java for fish.payara.microprofile.config declares
 * 'opens fish.payara.microprofile.config.cdi.model' so Mockito's byte-buddy
 * subclassing and JUnit's reflective access both work when tests run from the
 * unnamed module (classpath).
 */
public class ConfigPropertyModelTest {

    @Test
    public void nameFromAnnotation_usesAnnotationValue() {
        InjectionPoint ip = mockInjectionPoint("my.config.key", ConfigProperty.UNCONFIGURED_VALUE, "fieldName", SampleBean.class);
        ConfigPropertyModel model = new ConfigPropertyModel(ip);
        assertEquals("my.config.key", model.getName());
    }

    @Test
    public void nameFromAnnotation_withPrefix_prependsPrefix() {
        InjectionPoint ip = mockInjectionPoint("timeout", ConfigProperty.UNCONFIGURED_VALUE, "timeout", SampleBean.class);
        ConfigPropertyModel model = new ConfigPropertyModel(ip, "server.");
        assertEquals("server.timeout", model.getName());
    }

    @Test
    public void nameFromField_whenAnnotationNameEmpty_derivesFromMember() {
        InjectionPoint ip = mockInjectionPoint("", ConfigProperty.UNCONFIGURED_VALUE, "myField", SampleBean.class);
        ConfigPropertyModel model = new ConfigPropertyModel(ip);
        // Expected: fully-qualified class name + "." + field name
        String expected = SampleBean.class.getCanonicalName() + ".myField";
        assertEquals(expected, model.getName());
    }

    @Test
    public void nameFromField_withPrefix_prependsPrefix() {
        InjectionPoint ip = mockInjectionPoint("", ConfigProperty.UNCONFIGURED_VALUE, "port", SampleBean.class);
        ConfigPropertyModel model = new ConfigPropertyModel(ip, "db.");
        String expected = "db." + SampleBean.class.getCanonicalName() + ".port";
        assertEquals(expected, model.getName());
    }

    @Test
    public void defaultValue_isPreservedFromAnnotation() {
        InjectionPoint ip = mockInjectionPoint("key", "fallback", "fieldName", SampleBean.class);
        ConfigPropertyModel model = new ConfigPropertyModel(ip);
        assertEquals("fallback", model.getDefaultValue());
    }

    @Test
    public void defaultValue_isNullWhenNoAnnotation() {
        // InjectionPoint with no @ConfigProperty annotation
        InjectionPoint ip = mockInjectionPointWithoutAnnotation("fieldName", SampleBean.class);
        ConfigPropertyModel model = new ConfigPropertyModel(ip);
        assertNull(model.getDefaultValue());
    }

    @Test
    public void injectionPoint_isRetained() {
        InjectionPoint ip = mockInjectionPoint("key", "default", "fieldName", SampleBean.class);
        ConfigPropertyModel model = new ConfigPropertyModel(ip);
        assertEquals(ip, model.getInjectionPoint());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static InjectionPoint mockInjectionPoint(
            String propName, String defaultValue, String fieldName, Class beanClass) {

        ConfigProperty annotation = mock(ConfigProperty.class);
        when(annotation.name()).thenReturn(propName);
        when(annotation.defaultValue()).thenReturn(defaultValue);

        Annotated annotated = mock(Annotated.class);
        when(annotated.getAnnotation(ConfigProperty.class)).thenReturn(annotation);

        Member member = mock(Member.class);
        when(member.getName()).thenReturn(fieldName);
        when(member.getDeclaringClass()).thenReturn(beanClass);

        Bean bean = mock(Bean.class);
        when(bean.getBeanClass()).thenReturn(beanClass);

        InjectionPoint ip = mock(InjectionPoint.class);
        when(ip.getAnnotated()).thenReturn(annotated);
        when(ip.getMember()).thenReturn(member);
        when(ip.getBean()).thenReturn(bean);
        return ip;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static InjectionPoint mockInjectionPointWithoutAnnotation(String fieldName, Class beanClass) {
        Annotated annotated = mock(Annotated.class);
        when(annotated.getAnnotation(ConfigProperty.class)).thenReturn(null);

        Member member = mock(Member.class);
        when(member.getName()).thenReturn(fieldName);
        when(member.getDeclaringClass()).thenReturn(beanClass);

        InjectionPoint ip = mock(InjectionPoint.class);
        when(ip.getAnnotated()).thenReturn(annotated);
        when(ip.getMember()).thenReturn(member);
        when(ip.getBean()).thenReturn(null);
        return ip;
    }

    // Dummy class to use as a bean class in tests
    static class SampleBean {}
}
