/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
package fish.payara.monitoring.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import org.junit.Test;

/**
 * Tests basic correctness of {@link SeriesAnnotation} type.
 * 
 * @author Jan Bernitt
 */
public class SeriesAnnotationTest {

    @Test
    public void annotationAttributesMustBeGivenInPairs() {
        assertInvalidAttributes("OnlyName");
        assertInvalidAttributes("Name1", "Value1", "OnlyName2");
        assertInvalidAttributes("Name1", "Value1", "Name2", "Value2", "OnlyName2");

        assertValidAttributes();
        assertValidAttributes("Name1", "Value1");
        assertValidAttributes("Name1", "Value1", "Name2", "Value2");
        assertValidAttributes("Name1", "Value1", "Name2", "Value2", "Name3", "Value3");
    }

    @Test
    public void annotationAttributesCanBeIterated() {
        assertAttributesCanBeIterated();
        assertAttributesCanBeIterated("Key1", "Val1");
        assertAttributesCanBeIterated("Key1", "Val1", "Key2", "Val2");
        assertAttributesCanBeIterated("Key1", "Val1", "Key2", "Val2", "Key3", "Val3");
    }

    @Test
    public void annotationsWithKeyAreRecognised() {
        assertFalse(newAnnotation(false).isKeyed());
        assertFalse(newAnnotation(true).isKeyed());
        assertCanBeNonKeyed("Key1", "Val1");
        assertCanBeNonKeyed("Key1", "Val1", "Key2", "Val2");
        assertCanBeNonKeyed("Key1", "Val1", "Key2", "Val2", "Key3", "Val3");
        assertCanBeKeyed("Key1", "Val1");
        assertCanBeKeyed("Key1", "Val1", "Key2", "Val2");
        assertCanBeKeyed("Key1", "Val1", "Key2", "Val2", "Key3", "Val3");
    }

    private static void assertCanBeNonKeyed(String... attrs) {
        SeriesAnnotation annotation = newAnnotation(false, attrs);
        assertFalse(annotation.isKeyed());
        assertNull(annotation.getKeyAttribute());
    }

    private static void assertCanBeKeyed(String... attrs) {
        SeriesAnnotation annotation = newAnnotation(true, attrs);
        assertTrue(annotation.isKeyed());
        assertEquals(attrs[1], annotation.getKeyAttribute());
    }

    private static void assertAttributesCanBeIterated(String... attrs) {
        SeriesAnnotation annotation = newAnnotation(false, attrs);
        assertEquals(attrs.length / 2, annotation.getAttriuteCount());
        Iterator<Entry<String, String>> iter = annotation.iterator();
        for (int i = 0; i < attrs.length; i+=2) {
            assertTrue(iter.hasNext());
            Entry<String, String> attr = iter.next();
            assertEquals(attrs[i], attr.getKey());
            assertEquals(attrs[i + 1], attr.getValue());
        }
        assertFalse(iter.hasNext());
        try {
            iter.next(); // try anyway
            fail("Expected a NoSuchElementException as there should not be more elements");
        } catch (NoSuchElementException ex) {
            assertEquals("No more attribute available.", ex.getMessage());
        }
    }

    private static void assertInvalidAttributes(String...attrs) {
        String attrsString = Arrays.toString(attrs);
        try {
            assertNotNull(newAnnotation(false, attrs));
            fail("Expected attributes cause exception but were accepted: " + attrsString);
        } catch (IllegalArgumentException ex) {
            assertEquals("Annotation attributes always must be given in pairs but got: " + attrsString, ex.getMessage());
        }
    }

    private static void assertValidAttributes(String...attrs) {
        assertNotNull(newAnnotation(false, attrs));
    }

    public static SeriesAnnotation newAnnotation(boolean keyed, String... attrs) {
        return new SeriesAnnotation(1L, Series.ANY, "instance", 1L, keyed, attrs);
    }

}
