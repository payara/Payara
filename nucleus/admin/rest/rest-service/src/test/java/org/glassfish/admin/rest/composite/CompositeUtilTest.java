/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.customvalidators.ReferenceConstraint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;
import javax.validation.ConstraintViolation;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.composite.metadata.AttributeReference;
import org.glassfish.admin.rest.model.BaseModel;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author jdlee
 */
public class CompositeUtilTest {
    private static String json =
            "{\"name\":\"testModel\",\"count\":123, \"related\":[{\"id\":\"rel1\", \"description\":\"description 1\"},{\"id\":\"rel2\", \"description\":\"description 2\"}]}";

    @Test
    public void modelGeneration() {
        BaseModel model = CompositeUtil.instance().getModel(BaseModel.class);
        Assert.assertNotNull(model);
    }

    @Test
    public void readInJson() throws Exception {
        Locale locale = null;
        JSONObject o = new JSONObject(json);
        BaseModel model = CompositeUtil.instance().unmarshallClass(locale, BaseModel.class, o);

        Assert.assertEquals(model.getName(), "testModel");
        Assert.assertEquals(model.getRelated().size(), 2);
        Assert.assertTrue(model.getRelated().get(0).getDescription().startsWith("description "));
    }

    @Test
    public void testBeanValidationSupport() {
        Locale locale = null;
        final CompositeUtil cu = CompositeUtil.instance();
        BaseModel model = cu.getModel(BaseModel.class);
        model.setName(null); // Redundant, but here for emphasis
        model.setSize(16); // Must be between 10 and 15, inclusive
        model.setConfigRef(null); // Not null. Validation pulled in from the ConfigBean

        Set<ConstraintViolation<BaseModel>> violations = cu.validateRestModel(locale, model);
        Assert.assertEquals(3, violations.size());
    }

    @Test
    public void testAttributeReferenceProcessing() throws Exception {
        final CompositeUtil cu = CompositeUtil.instance();
        BaseModel model = cu.getModel(BaseModel.class);
        
        final Method clusterMethod = Cluster.class.getMethod("getConfigRef");
        final Method modelMethod = model.getClass().getDeclaredMethod("getConfigRef");

        Annotation[] fromCluster = clusterMethod.getAnnotations();
        Annotation[] fromRestModel = modelMethod.getAnnotations();

        Assert.assertEquals(fromCluster.length, fromRestModel.length);
        Assert.assertEquals(clusterMethod.getAnnotation(ReferenceConstraint.RemoteKey.class).message(),
                            modelMethod.getAnnotation(ReferenceConstraint.RemoteKey.class).message());
    }

    @Test
    public void testDirtyFieldDetection() throws JSONException {
        Locale locale = null;
        JSONObject o = new JSONObject(json);
        BaseModel model = CompositeUtil.instance().unmarshallClass(locale, BaseModel.class, o);
        RestModel rmi = (RestModel)model;

        Assert.assertTrue(rmi.isSet("name"));
        Assert.assertTrue(rmi.isSet("count"));
        Assert.assertTrue(rmi.isSet("related"));
        Assert.assertFalse(rmi.isSet("size"));
    }
}
