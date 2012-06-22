/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.composite;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author jdlee
 */
public class RestCollectionTest {
    RestCollection rc;

    public RestCollectionTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        rc = new RestCollection();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAdd() throws Exception {
        TestModel tm = CompositeUtil.getModel(TestModel.class, getClass(), null);
        Assert.assertNotNull(tm);

        rc.put("1", tm);
        Assert.assertEquals(1, rc.size());
        Assert.assertFalse(rc.isEmpty());
    }

    @Test
    public void testGet() throws Exception {
        TestModel tm = CompositeUtil.getModel(TestModel.class, getClass(), null);
        Assert.assertNotNull(tm);

        rc.put("1", tm);

        RestModel rm = rc.get("1");
        Assert.assertEquals(tm, rm);
    }

    @Test
    public void testRemove() throws Exception {
        TestModel tm = CompositeUtil.getModel(TestModel.class, getClass(), null);
        Assert.assertNotNull(tm);

        rc.put("1", tm);
        Assert.assertEquals(1, rc.size());
        Assert.assertFalse(rc.isEmpty());
        rc.remove("1");
        Assert.assertEquals(0, rc.size());
        Assert.assertTrue(rc.isEmpty());
    }

    @Test
    public void testContainsKey() throws Exception {
        TestModel tm = CompositeUtil.getModel(TestModel.class, getClass(), null);
        Assert.assertNotNull(tm);

        rc.put("1", tm);
        Assert.assertEquals(1, rc.size());
        Assert.assertFalse(rc.isEmpty());
        Assert.assertTrue(rc.containsKey("1"));
    }

    @Test
    public void testContainsValue() throws Exception {
        TestModel tm = CompositeUtil.getModel(TestModel.class, getClass(), null);
        Assert.assertNotNull(tm);

        rc.put("1", tm);
        Assert.assertEquals(1, rc.size());
        Assert.assertTrue(rc.containsValue(tm));
    }

    @Test
    public void testClear() throws Exception {
        TestModel tm = CompositeUtil.getModel(TestModel.class, getClass(), null);
        Assert.assertNotNull(tm);

        rc.put("1", tm);
        Assert.assertEquals(1, rc.size());
        rc.clear();
        Assert.assertEquals(0, rc.size());
        Assert.assertTrue(rc.isEmpty());
    }

    @Test
    public void testGetKeySet() throws Exception {
        TestModel tm = CompositeUtil.getModel(TestModel.class, getClass(), null);
        Assert.assertNotNull(tm);

        rc.put("1", tm);
        Assert.assertTrue(rc.keySet().contains(new RestModelMetadata("1")));
    }

    @Test
    public void testGetValues() throws Exception {
        TestModel tm = CompositeUtil.getModel(TestModel.class, getClass(), null);
        Assert.assertNotNull(tm);

        rc.put("1", tm);
        Assert.assertEquals(1, rc.values().size());
    }

    @Test
    public void testEntrySet()  throws Exception {
        TestModel tm = CompositeUtil.getModel(TestModel.class, getClass(), null);
        Assert.assertNotNull(tm);
        tm.setName("one");
        rc.put("1", tm);
        tm = CompositeUtil.getModel(TestModel.class, getClass(), null);
        tm.setName("two");
        rc.put("2", tm);

        Set<Map.Entry<RestModelMetadata, RestModel>> entries = rc.entrySet();
        Assert.assertEquals(2, entries.size());
        // Test contents...
    }

    public interface TestModel extends RestModel {
        public String getName();
        public void setName(String name);
    }
}
