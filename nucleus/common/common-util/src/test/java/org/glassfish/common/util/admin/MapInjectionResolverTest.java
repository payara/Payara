/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.common.util.admin;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.glassfish.api.ExecutionContext;
import org.glassfish.api.Param;
import org.glassfish.api.ParamDefaultCalculator;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.ParameterMap;
import static org.junit.Assert.*;
import org.junit.Test;


/**
 * junit test to test MapInjectionResolver class
 */
public class MapInjectionResolverTest {

    @Test
    public void getParameterValueTest() {

        ParameterMap params = new ParameterMap();
        params.set("foo", "bar");
        params.set("hellO", "world");
        params.set("one", "two");
        params.set("thrEE", "Four");
        params.set("FivE", "six");
        params.set("sIx", "seVen");
        params.set("eiGHT", "niNe");
        String value =
            MapInjectionResolver.getParameterValue(params, "foo", false);
        assertEquals("value is bar", "bar", value);
        value = MapInjectionResolver.getParameterValue(params, "hello", true);
        assertEquals("value is world", "world", value);        
        value = MapInjectionResolver.getParameterValue(params, "onE", true);
        assertEquals("value is two", "two", value);
        value = MapInjectionResolver.getParameterValue(params, "three", true);
        assertEquals("value is four", "Four", value);                
        value = MapInjectionResolver.getParameterValue(params, "five", false);
        assertEquals("value is null", null, value);
        value = MapInjectionResolver.getParameterValue(params, "six", true);
        assertEquals("value is SeVen", "seVen", value);
        value = MapInjectionResolver.getParameterValue(params, "eight", true);
        assertEquals("value is niNe", "niNe", value);
        value = MapInjectionResolver.getParameterValue(params, "none", true);
        assertEquals("value is null", null, value);        
    }

    @Test
    public void convertStringToPropertiesTest() {
        String propsStr = "prop1=valA:prop2=valB:prop3=valC";
        Properties propsExpected = new Properties();
        propsExpected.put("prop1", "valA");
        propsExpected.put("prop2", "valB");
        propsExpected.put("prop3", "valC");
        Properties propsActual =
            MapInjectionResolver.convertStringToProperties(propsStr, ':');
        assertEquals(propsExpected, propsActual);
    }
    
    @Test
    public void parsePropertiesEscapeCharTest() {
        String propsStr = "connectionAttributes=\\;create\\\\\\=true";
        Properties propsExpected = new Properties();
        propsExpected.put("connectionAttributes", ";create\\=true");
        Properties propsActual = null;
        propsActual =
            MapInjectionResolver.convertStringToProperties(propsStr, ':');
        assertEquals(propsExpected, propsActual);
    }
    
    @Test
    public void parsePropertiesEscapeCharTest2() {
        String propsStr = "connectionAttributes=;create\\=true";
        Properties propsExpected = new Properties();
        propsExpected.put("connectionAttributes", ";create=true");
        Properties propsActual = null;
        propsActual =
            MapInjectionResolver.convertStringToProperties(propsStr, ':');
        assertEquals(propsExpected, propsActual);
    }

    @Test
    public void parsePropertiesQuoteTest() {
        String propsStr =
            "java.naming.provider.url=\"ldap://ldapserver.sun.com:389\":" +
            "java.naming.security.authentication=simple:" +
            "java.naming.security.credentials=changeit:" +
            "java.naming.security.principal=\"uid=admin,ou=People," +
                "o=foo,o=something\"";
        Properties propsExpected = new Properties();
        propsExpected.put("java.naming.provider.url",
                                "ldap://ldapserver.sun.com:389");
        propsExpected.put("java.naming.security.authentication", "simple");
        propsExpected.put("java.naming.security.credentials", "changeit");
        propsExpected.put("java.naming.security.principal",
                                "uid=admin,ou=People,o=foo,o=something");
        Properties propsActual =
            MapInjectionResolver.convertStringToProperties(propsStr, ':');
        assertEquals(propsExpected, propsActual);
    }
    
    @Test
    public void convertStringToObjectTest() throws Exception {
        DummyCommand dc = new DummyCommand();
        Class<?> cl = dc.getClass();
        AnnotatedElement target = (AnnotatedElement)cl.getDeclaredField("foo");
        String paramValueStr = "prop1=valA:prop2=valB:prop3=valC";
        Object paramValActual = MapInjectionResolver.convertStringToObject(
                                    target, String.class, paramValueStr);
        Object paramValExpected =  "prop1=valA:prop2=valB:prop3=valC";
        assertEquals("String type", paramValExpected, paramValActual);
  
        target = (AnnotatedElement)cl.getDeclaredField("prop");
        paramValActual = MapInjectionResolver.convertStringToObject(
                                    target, Properties.class, paramValueStr);
        paramValExpected = new Properties();        
        ((Properties)paramValExpected).put("prop1", "valA");
        ((Properties)paramValExpected).put("prop2", "valB");
        ((Properties)paramValExpected).put("prop3", "valC");
        assertEquals("Properties type", paramValExpected, paramValActual);

        target = (AnnotatedElement)cl.getDeclaredField("portnum");
        paramValueStr = "8080";
        paramValActual = MapInjectionResolver.convertStringToObject(
                                    target, Integer.class, paramValueStr);
        paramValExpected = new Integer(8080);
        assertEquals("Integer type", paramValExpected, paramValActual);

        paramValueStr = "server1:server2:server3";
        target = (AnnotatedElement)cl.getDeclaredField("lstr");
        paramValActual = MapInjectionResolver.convertStringToObject(
                                    target, List.class, paramValueStr);
        List<String> paramValueList = new java.util.ArrayList();
        paramValueList.add("server1");
        paramValueList.add("server2");
        paramValueList.add("server3");
        assertEquals("List type", paramValueList, paramValActual);

        paramValueStr = "server1,server2,server3";
        target = (AnnotatedElement)cl.getDeclaredField("astr");
        paramValActual = MapInjectionResolver.convertStringToObject(
                                    target, (new String[]{}).getClass(),
                                                  paramValueStr);
        String[] strArray = new String[3];
        strArray[0] = "server1";
        strArray[1] = "server2";
        strArray[2] = "server3";
        assertEquals("String Array type", strArray, (String[])paramValActual);
    }

    @Test
    public void convertListToObjectTest() throws Exception {
        DummyCommand dc = new DummyCommand();
        Class<?> cl = dc.getClass();
        AnnotatedElement target =
            (AnnotatedElement)cl.getDeclaredField("propm");
        List<String> paramValueList = new ArrayList<String>();
        paramValueList.add("prop1=valA");
        paramValueList.add("prop2=valB");
        paramValueList.add("prop3=valC");
        Object paramValActual = MapInjectionResolver.convertListToObject(
                                    target, Properties.class, paramValueList);
        Object paramValExpected = new Properties();        
        ((Properties)paramValExpected).put("prop1", "valA");
        ((Properties)paramValExpected).put("prop2", "valB");
        ((Properties)paramValExpected).put("prop3", "valC");
        assertEquals("Properties type", paramValExpected, paramValActual);

        paramValueList.clear();
        paramValueList.add("server1");
        paramValueList.add("server2");
        paramValueList.add("server3");
        target = (AnnotatedElement)cl.getDeclaredField("lstrm");
        paramValActual = MapInjectionResolver.convertListToObject(
                                    target, List.class, paramValueList);
        assertEquals("List type", paramValueList, paramValActual);

        target = (AnnotatedElement)cl.getDeclaredField("astrm");
        paramValActual = MapInjectionResolver.convertListToObject(
                                    target, (new String[]{}).getClass(),
                                                  paramValueList);
        String[] strArray = new String[3];
        strArray[0] = "server1";
        strArray[1] = "server2";
        strArray[2] = "server3";
        assertEquals("String Array type", strArray, (String[])paramValActual);
    }

    @Test
    public void getParamValueStringTest() {
        try {
            DummyCommand dc = new DummyCommand();
            Class<?> cl = dc.getClass();
            AnnotatedElement ae = (AnnotatedElement)cl.getDeclaredField("foo");
            Param param = ae.getAnnotation(Param.class);
            ParameterMap params = new ParameterMap();
            params.set("foo", "true");
            String val =
                MapInjectionResolver.getParamValueString(params, param, ae, null);
            assertEquals("val should be true", "true", val);

            ae = (AnnotatedElement)cl.getDeclaredField("bar");
            param = ae.getAnnotation(Param.class);
            val = MapInjectionResolver.getParamValueString(params, param, ae, null);
            assertEquals("val should be false", "false", val);

            ae = (AnnotatedElement)cl.getDeclaredField("hello");
            param = ae.getAnnotation(Param.class);
            val = MapInjectionResolver.getParamValueString(params, param, ae, null);
            assertEquals("val should be null", null, val);
            
            ae = (AnnotatedElement)cl.getDeclaredField("dyn");
            param = ae.getAnnotation(Param.class);
            val = MapInjectionResolver.getParamValueString(params, param, ae, null);
            assertEquals("val should be dynamic-default-value", "dynamic-default-value", val);
            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Test
    public void getParamFieldTest() {
        try {
            DummyCommand dc = new DummyCommand();
            Class<?> cl = dc.getClass();
            AnnotatedElement ae =
                (AnnotatedElement)cl.getDeclaredField("hello");
            Object obj = MapInjectionResolver.getParamField(dc, ae);
            assertEquals("obj should be world", "world", (String)obj);
            ae = (AnnotatedElement)cl.getDeclaredField("prop");
            obj = MapInjectionResolver.getParamField(dc, ae);
            assertEquals("obj should be null", null, obj);   
            
            ae = (AnnotatedElement)cl.getDeclaredField("dyn3");
            obj = MapInjectionResolver.getParamField(dc, ae);
            assertEquals("obj should be dynamic-default-value", "dynamic-default-value", (String)obj);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void getParamValueTest() {
        try {
            DummyCommand dc = new DummyCommand();
            Class<?> cl = dc.getClass();
            ParameterMap params = new ParameterMap();
            params.add("hello", "world");
            
            CommandModel dccm = new CommandModelImpl(dc.getClass());
            MapInjectionResolver mir = new MapInjectionResolver(dccm, params);
            
            AnnotatedElement ae =
                (AnnotatedElement)cl.getDeclaredField("hello");
            String hello = mir.getValue(dc, ae, null, String.class);
            assertEquals("hello should be world", "world", hello);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail("unexpected exception");
        } 
    }
    
    @Test
    public void convertStringToListTest() {
        String listStr = "server1\\:server2:\\\\server3:server4";
        List<String> listExpected = new java.util.ArrayList();
        listExpected.add("server1:server2");
        listExpected.add("\\server3");
        listExpected.add("server4");
        List<String> listActual =
            MapInjectionResolver.convertStringToList(listStr, ':');
        assertEquals(listExpected, listActual);
    }

    @Test
    public void convertStringToStringArrayTest() {
        String strArray = "server1\\,server2,\\\\server3,server4";
        String[] strArrayExpected = new String[3];
        strArrayExpected[0]="server1,server2";
        strArrayExpected[1]="\\server3";
        strArrayExpected[2]="server4";
        String[] strArrayActual =
            MapInjectionResolver.convertStringToStringArray(strArray, ',');
        assertEquals(strArrayExpected, strArrayActual);
    }
    
    public static class DynTest extends ParamDefaultCalculator {
        public DynTest() {}
        @Override
        public String defaultValue(ExecutionContext ec) {
            return "dynamic-default-value";
        }
    }
    
    public static class DynCalculator {
        public static String getDefault() {
            return "dynamic-default-value";
        }
    }


        //mock-up DummyCommand object
    public class DummyCommand {
        @Param(name="foo")
        String foo;
        @Param(name="bar", defaultValue="false")
        String bar;
        @Param
        String hello="world";
        @Param
        int portnum;
        @Param(name="prop", separator=':')
        Properties prop;
        @Param(name="lstr", separator=':')
        List<String> lstr;
        @Param(name="astr")
        String[] astr;
        @Param(name="propm", multiple=true)
        Properties propm;
        @Param(name="lstrm", multiple=true)
        List<String> lstrm;
        @Param(name="astrm", multiple=true)
        String[] astrm;
        
        @Param(name="dyn", optional=true, defaultCalculator=DynTest.class)
        String dyn;
        
        @Param(optional=true)
        String dyn3 = DynCalculator.getDefault();
    }
}
