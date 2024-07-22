/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.monitoring.rest.app.handler;

import fish.payara.monitoring.rest.app.MBeanServerDelegate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


public class MBeanAttributeReadHandlerTest {

    private static final String BEAN_NAME = "someName";
    private static final String BEAN_ATTRIBUTE = "someAttribute";

    @Mock
    private MBeanServerDelegate delegateMock;

    @Before
    public void setup() {
        delegateMock = Mockito.mock(MBeanServerDelegate.class);
    }

    @Test
    public void getValueObject_String() throws Exception {
        MBeanAttributeReadHandler handler = new MBeanAttributeReadHandler(delegateMock, BEAN_NAME, BEAN_ATTRIBUTE);
        when(delegateMock.getMBeanAttribute(BEAN_NAME, BEAN_ATTRIBUTE)).thenReturn("Payara");

        JsonValue value = handler.getValueObject();
        assertThat(value.getValueType()).isEqualTo(JsonValue.ValueType.STRING);
        assertThat(((JsonString) value).getString()).isEqualTo("Payara");
    }

    @Test
    public void getValueObject_Long() throws Exception {
        MBeanAttributeReadHandler handler = new MBeanAttributeReadHandler(delegateMock, BEAN_NAME, BEAN_ATTRIBUTE);
        when(delegateMock.getMBeanAttribute(BEAN_NAME, BEAN_ATTRIBUTE)).thenReturn(123L);

        JsonValue value = handler.getValueObject();
        assertThat(value.getValueType()).isEqualTo(JsonValue.ValueType.NUMBER);
        assertThat(((JsonNumber) value).longValue()).isEqualTo(123L);
    }

    @Test
    public void getValueObject_Boolean() throws Exception {
        MBeanAttributeReadHandler handler = new MBeanAttributeReadHandler(delegateMock, BEAN_NAME, BEAN_ATTRIBUTE);
        when(delegateMock.getMBeanAttribute(BEAN_NAME, BEAN_ATTRIBUTE)).thenReturn(Boolean.TRUE);

        JsonValue value = handler.getValueObject();
        assertThat(value).isEqualTo(JsonValue.TRUE);

    }

    @Test
    public void getValueObject_Array() throws Exception {
        MBeanAttributeReadHandler handler = new MBeanAttributeReadHandler(delegateMock, BEAN_NAME, BEAN_ATTRIBUTE);
        when(delegateMock.getMBeanAttribute(BEAN_NAME, BEAN_ATTRIBUTE)).thenReturn(new Integer[]{1,2,3});

        JsonValue value = handler.getValueObject();
        assertThat(value.getValueType()).isEqualTo(JsonValue.ValueType.ARRAY);
        List<JsonNumber> data = ((JsonArray) value).getValuesAs(JsonNumber.class);
        assertThat(data.stream().map(JsonNumber::intValue)).containsExactly(1, 2, 3);
    }

    @Test
    public void getValueObject_null() throws Exception {
        MBeanAttributeReadHandler handler = new MBeanAttributeReadHandler(delegateMock, BEAN_NAME, BEAN_ATTRIBUTE);
        when(delegateMock.getMBeanAttribute(BEAN_NAME, BEAN_ATTRIBUTE)).thenReturn(null);

        JsonValue value = handler.getValueObject();
        System.out.println(value);
    }
}