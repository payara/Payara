/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admingui.devtests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.glassfish.admingui.common.util.JSONUtil;
import static org.junit.Assert.*;

public class JsonUtilTest {
    @Test
    public void readNumbers() {
         assertEquals(3L, JSONUtil.jsonToJava("3"));
    }
    
    @Test 
    public void readConstants() {
        assertTrue((Boolean)JSONUtil.jsonToJava("true"));
        assertFalse((Boolean)JSONUtil.jsonToJava("false"));
        assertNull(JSONUtil.jsonToJava("null"));
    }
    
    @Test
    public void readLists() {
        List list = (List)JSONUtil.jsonToJava("[null]");
        assertNotNull(list);
        assertEquals(1, list.size());
        assertNull(list.get(0));
        
        list = (List)JSONUtil.jsonToJava("[true,false,null,{'a':'b'},['1','2','3']]");
        assertNotNull(list);
        assertEquals(5, list.size());
        assertTrue((Boolean)list.get(0));
        assertFalse((Boolean)list.get(1));
        assertNull(list.get(2));
        assertTrue(list.get(3) instanceof Map);
        assertEquals("b", ((Map)list.get(3)).get("a"));
        assertTrue(list.get(4) instanceof List);
        assertEquals("1", ((List)list.get(4)).get(0));

        list = (List)JSONUtil.jsonToJava("[true,false,null,{'a':'b'},[1,2,3]]");
        assertNotNull(list);
        assertEquals(5, list.size());
        assertEquals(1L, ((List)list.get(4)).get(0));

        list = (List)JSONUtil.jsonToJava("[true,false,null,{'a':'b'},[1.1,2.2,3.3]]");
        assertNotNull(list);
        assertEquals(5, list.size());
        assertEquals(1.1F, ((List)list.get(4)).get(0));

        list = (List)JSONUtil.jsonToJava("[true,false,null,{'a':'b'},['1',2,3.3]]");
        assertNotNull(list);
        assertEquals(5, list.size());
        assertEquals("1", ((List)list.get(4)).get(0));
        assertEquals(2L, ((List)list.get(4)).get(1));
        assertEquals(3.3F, ((List)list.get(4)).get(2));
    }
    
    @Test
    public void readObjects() {
        Map<String, Object> map = (Map<String, Object>)JSONUtil.jsonToJava("{'x':['foo',null ,{'a':true, 'b':false }]}");
        assertEquals(1, map.size());
        assertTrue(map.get("x") instanceof List);
        assertEquals(3, ((List)map.get("x")).size());
        assertTrue(((List)map.get("x")).get(2) instanceof Map);
        
        map = (Map<String, Object>)JSONUtil.jsonToJava("{            'key'   :        \"value\" ,\n  \r \"key2\"   :   {  'innerKey'  : [  3.3E-2 , false  , 800e+8, null , 37  , \"test\" ] , \n \"innerKey2\" : {'a' : 'b', 'c' : 'd'}, 'innerKey3' : true} }");
        assertEquals(2, map.size());
        assertEquals("value", map.get("key"));
        assertTrue(map.get("key2") instanceof Map);
    }
    
    @Test
    public void testEncoding() {
        Map map = new HashMap<String, String>() {{ put("foo", "bar"); }};
        assertEquals("{\"foo\":\"bar\"}", JSONUtil.javaToJSON(map, 2));
    }
    
    @Test
    public void multibyteCharacters() {
        String json = "{\"value\":\"這或是因\"}";
        Map<String, Object> obj = (Map<String, Object>)JSONUtil.jsonToJava(json);
        assertEquals("這或是因", obj.get("value"));
    }
}
