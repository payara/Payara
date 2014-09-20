/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
package com.sun.enterprise.util;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author martinmares
 */
public class BeanUtilsTest {
    
    public static class Bean1 {
        
        private String one;
        private boolean two;
        private int three;

        public String getOne() {
            return one;
        }

        public void setOne(String one) {
            this.one = one;
        }

        public boolean isTwo() {
            return two;
        }

        public void setTwo(boolean two) {
            this.two = two;
        }
        
        public void setTwo(boolean two, String neco) {
            this.two = two;
        }

        public int getThree() {
            return three;
        }
        
        public int someMethod() {
            return 0;
        }
    }
    
    public BeanUtilsTest() {
    }

    @Test
    public void testBeanToMap() throws Exception {
        Bean1 b = new Bean1();
        b.setOne("1");
        b.setTwo(true);
        Map<String, Object> map = BeanUtils.beanToMap(b);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("1", map.get("one"));
        assertTrue((Boolean) map.get("two"));
        assertNotNull(map.get("three"));
    }

    @Test
    public void testMapToBean1() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("one", "hello");
        map.put("two", true);
        Bean1 b = new Bean1();
        BeanUtils.mapToBean(b, map, false);
        assertEquals("hello", b.getOne());
        assertTrue(b.isTwo());
    }
    
    @Test
    public void testMapToBean2() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("one", "hello");
        map.put("two", true);
        map.put("some", "some");
        Bean1 b = new Bean1();
        BeanUtils.mapToBean(b, map, true);
        try {
            BeanUtils.mapToBean(b, map, false);
            assertTrue(false);
        } catch (IllegalArgumentException ex) {
        }
    }
    
    @Test
    public void testGetGetters() throws Exception {
        Bean1 b = new Bean1();
        Collection<Method> getters = BeanUtils.getGetters(b);
        assertNotNull(getters);
        assertEquals(3, getters.size());
    }
    
    @Test
    public void testGetSetters() throws Exception {
        Bean1 b = new Bean1();
        Collection<Method> setters = BeanUtils.getSetters(b);
        assertNotNull(setters);
        assertEquals(2, setters.size());
    }

}