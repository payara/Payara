/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.impl.model.util;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class ModelUtilsTest {

    private static final String CURRENT_VALUE = "whatever";
    private static final String NEW_VALUE = "something else";
    private static final String NULL = null;

    /**
     * Tests that the function overrides properties correctly.
     */
    @Test
    public void mergePropertyTest() {
        assertEquals(NEW_VALUE, mergeProperty(CURRENT_VALUE, NEW_VALUE, true));
        assertEquals(CURRENT_VALUE, mergeProperty(CURRENT_VALUE, NEW_VALUE, false));
        assertEquals(CURRENT_VALUE, mergeProperty(CURRENT_VALUE, NULL, true));
        assertEquals(CURRENT_VALUE, mergeProperty(CURRENT_VALUE, NULL, false));
        assertEquals(NEW_VALUE, mergeProperty(NULL, NEW_VALUE, true));
        assertEquals(NEW_VALUE, mergeProperty(NULL, NEW_VALUE, false));
        assertEquals(NULL, mergeProperty(NULL, NULL, true));
        assertEquals(NULL, mergeProperty(NULL, NULL, false));
    }

    enum TestEnum {

        DEFAULT,
        OTHER
    }

    @Target(ElementType.ANNOTATION_TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface NestedAnnotation {
        String value() default "";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface TestAnnotation {
        String[] arrayAttribute() default {};
        boolean booleanAttribute() default false;
        Class<?> classAttribute() default Void.class;
        TestEnum enumAttribute() default TestEnum.DEFAULT;
        String stringAttribute() default "";
        NestedAnnotation annotationAttribute() default @NestedAnnotation();
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MarkerAnnotation { 
        // has no attributes
    }

    @TestAnnotation
    private Object nullAnnotation;
    @TestAnnotation(arrayAttribute = { "a" })
    private Object annotationWithArray;
    @TestAnnotation(booleanAttribute = true)
    private Object annotationWithBoolean;
    @TestAnnotation(classAttribute = String.class)
    private Object annotationWithClass;
    @TestAnnotation(enumAttribute = TestEnum.OTHER)
    private Object annoationWithEnum;
    @TestAnnotation(annotationAttribute = @NestedAnnotation("value"))
    private Object annoationWithAnnoation;
    @MarkerAnnotation
    private Object markerAnnoation;

    @Test
    public void nullAnnoationIsNull() {
        assertTrue(isAnnotationNull(null));
    }

    @Test
    public void emptyAnnotationIsNull() {
        assertTrue(isAnnotationNull(getFieldAnnotation("nullAnnotation")));
    }

    @Test
    public void markerAnnoationIsNull() {
        assertTrue(isAnnotationNull(getFieldAnnotation("markerAnnoation", MarkerAnnotation.class)));
    }

    @Test
    public void annotationWithNonEmptyArrayIsNotNull() {
        assertFalse(isAnnotationNull(getFieldAnnotation("annotationWithArray")));
    }

    @Test
    public void annotationWithNonFalseBooleanIsNotNull() {
        assertFalse(isAnnotationNull(getFieldAnnotation("annotationWithBoolean")));
    }

    @Test
    public void annotationWithNonVoidClassIsNotNull() {
        assertFalse(isAnnotationNull(getFieldAnnotation("annotationWithClass")));
    }

    @Test
    public void annotationWithNonDefaultEnumIsNotNull() {
        assertFalse(isAnnotationNull(getFieldAnnotation("annoationWithEnum")));
    }

    @Test
    public void annotationWithNonNullAnnoationIsNotNull() {
        assertFalse(isAnnotationNull(getFieldAnnotation("annoationWithAnnoation")));
    }

    private TestAnnotation getFieldAnnotation(String fieldName) {
        return getFieldAnnotation(fieldName, TestAnnotation.class);
    }

    private <T extends Annotation> T getFieldAnnotation(String fieldName, Class<T> annoationType) {
        try {
            T annotation = getClass().getDeclaredField(fieldName).getAnnotation(annoationType);
            assertNotNull(annotation);
            return annotation;
        } catch (NoSuchFieldException | SecurityException e) {
            fail("Field expected: "+fieldName);
            return null;
        }
    }
}